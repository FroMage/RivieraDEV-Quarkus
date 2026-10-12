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
 * To record the expected image of something that changed on purpose, run the test with -Dgolden.update=true,
 * then look at the result and commit it.
 */
public class Images {

    private static final Path GOLDEN_DIRECTORY = Path.of("src/test/resources/golden");
    private static final Path DIFFERENCES_DIRECTORY = Path.of("target/golden-differences");

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
     * Checks that an image looks like the golden one of that name.
     *
     * @param tolerance the largest difference that we accept, see {@link #difference}
     */
    public static void assertLooksLike(String name, byte[] actual, double tolerance) {
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
            double difference = difference(expected, actual);
            if (difference > tolerance) {
                Files.createDirectories(DIFFERENCES_DIRECTORY);
                Files.write(DIFFERENCES_DIRECTORY.resolve(name + ".png"), actual);
                fail("Image " + name + " differs from the golden one by " + difference + " (more than " + tolerance
                        + "), the image we got is in " + DIFFERENCES_DIRECTORY.resolve(name + ".png"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
