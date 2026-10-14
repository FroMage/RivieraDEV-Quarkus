package testdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Comparison of rendered images with the ones we expect ("golden" images, in src/test/resources/golden).
 *
 * Machines do not rasterise text the same way (hinting, anti-aliasing, the FreeType they use), which moves pixels of
 * the glyphs around, so the images are compared by the average colour of blocks of {@link #BLOCK} x {@link #BLOCK}
 * pixels. That ignores how the strokes of the letters are drawn, but still notices when a font, an image, the layout or
 * a text changes.
 *
 * To record the expected image of something that changed on purpose, run the test with -Dgolden.update=true,
 * then look at the result and commit it.
 */
public class Images {

    private static final Path GOLDEN_DIRECTORY = Path.of("src/test/resources/golden");
    private static final Path DIFFERENCES_DIRECTORY = Path.of("target/golden-differences");

    /** Size of the blocks whose average colour is compared */
    public static final int BLOCK = 16;

    /** The largest root mean square difference of the average colours of the blocks, from 0 to 255 */
    public static final double TOLERANCE = 3.5;

    /** The largest difference of the average colour of any block, from 0 to 255: a missing word or image is more */
    public static final double LOCAL_TOLERANCE = 45;

    /** How much two images differ when compared by blocks, see {@link #BLOCK} */
    public record Coarse(double rootMeanSquare, double largestBlock) {
        public boolean within(double tolerance, double localTolerance) {
            return rootMeanSquare <= tolerance && largestBlock <= localTolerance;
        }
    }

    public static Coarse coarseDifference(byte[] expected, byte[] actual) {
        BufferedImage a = read(expected);
        BufferedImage b = read(actual);
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return new Coarse(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
        int blocksX = a.getWidth() / BLOCK;
        int blocksY = a.getHeight() / BLOCK;
        double sum = 0;
        double largest = 0;
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                double[] meanA = mean(a, bx * BLOCK, by * BLOCK);
                double[] meanB = mean(b, bx * BLOCK, by * BLOCK);
                for (int channel = 0; channel < 3; channel++) {
                    double d = Math.abs(meanA[channel] - meanB[channel]);
                    sum += d * d;
                    largest = Math.max(largest, d);
                }
            }
        }
        return new Coarse(Math.sqrt(sum / (3.0 * blocksX * blocksY)), largest);
    }

    private static double[] mean(BufferedImage image, int x0, int y0) {
        double[] sum = new double[3];
        for (int y = y0; y < y0 + BLOCK; y++) {
            for (int x = x0; x < x0 + BLOCK; x++) {
                int p = image.getRGB(x, y);
                sum[0] += (p >> 16) & 255;
                sum[1] += (p >> 8) & 255;
                sum[2] += p & 255;
            }
        }
        for (int channel = 0; channel < 3; channel++) {
            sum[channel] /= BLOCK * BLOCK;
        }
        return sum;
    }

    /** Root mean square difference of the colour channels, from 0 (identical) to 255 */
    public static double difference(byte[] expected, byte[] actual) {
        BufferedImage a = read(expected);
        BufferedImage b = read(actual);
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                for (int shift = 0; shift <= 16; shift += 8) {
                    int d = ((p >> shift) & 255) - ((q >> shift) & 255);
                    sum += d * d;
                }
            }
        }
        return Math.sqrt(sum / (3.0 * a.getWidth() * a.getHeight()));
    }

    /**
     * Checks that an image looks like the golden one of that name, see {@link #BLOCK}.
     */
    public static void assertLooksLike(String name, byte[] actual) {
        assertLooksLike(name, actual, TOLERANCE, LOCAL_TOLERANCE);
    }

    public static void assertLooksLike(String name, byte[] actual, double tolerance, double localTolerance) {
        Path golden = GOLDEN_DIRECTORY.resolve(name + ".png");
        try {
            if (Boolean.getBoolean("golden.update")) {
                Files.createDirectories(GOLDEN_DIRECTORY);
                Files.write(golden, actual);
                return;
            }
            byte[] expected;
            try (InputStream in = Images.class.getResourceAsStream("/golden/" + name + ".png")) {
                assertNotNull(in, "No golden image for " + name + ": run with -Dgolden.update=true to record it");
                expected = in.readAllBytes();
            }
            Coarse coarse = coarseDifference(expected, actual);
            if (!coarse.within(tolerance, localTolerance)) {
                Files.createDirectories(DIFFERENCES_DIRECTORY);
                Files.write(DIFFERENCES_DIRECTORY.resolve(name + ".png"), actual);
                Files.write(DIFFERENCES_DIRECTORY.resolve(name + "-expected.png"), expected);
                Files.write(DIFFERENCES_DIRECTORY.resolve(name + "-differences.png"), differencesImage(expected, actual));
                fail("Image " + name + " differs from the golden one: by " + coarse.rootMeanSquare() + " on average (at most "
                        + tolerance + ") and by " + coarse.largestBlock() + " in the worst " + BLOCK + "x" + BLOCK
                        + " block (at most " + localTolerance + "); " + describeDifferences(expected, actual)
                        + ". Look at " + name + ".png (what we got), " + name + "-expected.png and " + name
                        + "-differences.png in " + DIFFERENCES_DIRECTORY);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Where the images differ visibly, to tell what changed: "123 pixels in x 40..300, y 200..450" */
    public static String describeDifferences(byte[] expected, byte[] actual) {
        BufferedImage a = read(expected);
        BufferedImage b = read(actual);
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return "the size is " + b.getWidth() + "x" + b.getHeight() + " instead of " + a.getWidth() + "x" + a.getHeight();
        }
        int count = 0;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (largestChannelDifference(a.getRGB(x, y), b.getRGB(x, y)) > VISIBLE) {
                    count++;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (count == 0) {
            return "no pixel differs visibly (by more than " + VISIBLE + "), the difference is spread thinly";
        }
        return count + " of " + (a.getWidth() * a.getHeight()) + " pixels differ visibly (by more than " + VISIBLE
                + "), in x " + minX + ".." + maxX + ", y " + minY + ".." + maxY + " of " + a.getWidth() + "x"
                + a.getHeight();
    }

    /** An image where the pixels that differ are bright, and the ones that are the same are black */
    public static byte[] differencesImage(byte[] expected, byte[] actual) {
        BufferedImage a = read(expected);
        BufferedImage b = read(actual);
        int width = Math.min(a.getWidth(), b.getWidth());
        int height = Math.min(a.getHeight(), b.getHeight());
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int d = Math.min(255, largestChannelDifference(a.getRGB(x, y), b.getRGB(x, y)) * 4);
                result.setRGB(x, y, (d << 16) | (d << 8) | d);
            }
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            ImageIO.write(result, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static final int VISIBLE = 24;

    private static int largestChannelDifference(int p, int q) {
        int largest = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            largest = Math.max(largest, Math.abs(((p >> shift) & 255) - ((q >> shift) & 255)));
        }
        return largest;
    }

    public static void assertIsPng(byte[] bytes) {
        assertEquals(0x89, bytes[0] & 0xFF);
        assertEquals('P', bytes[1]);
        assertEquals('N', bytes[2]);
        assertEquals('G', bytes[3]);
    }

    private static BufferedImage read(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            assertNotNull(image, "Not an image");
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
