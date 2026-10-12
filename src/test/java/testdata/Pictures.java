package testdata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/**
 * Pictures that are always the same for a given seed, so that what is rendered from them can be compared.
 */
public class Pictures {

    private static final Color[][] PALETTES = {
            { new Color(0xE63946), new Color(0xF1FAEE) },
            { new Color(0x2A9D8F), new Color(0xE9C46A) },
            { new Color(0x6A4C93), new Color(0xFFCA3A) },
            { new Color(0x1982C4), new Color(0x8AC926) },
    };

    /** A portrait: a gradient with a head and shoulders */
    public static byte[] photo(int seed) {
        return draw(seed, 600, 800, true);
    }

    /** A logo: a gradient with a disc */
    public static byte[] logo(int seed) {
        return draw(seed, 200, 80, false);
    }

    private static byte[] draw(int seed, int width, int height, boolean portrait) {
        Color[] palette = PALETTES[Math.floorMod(seed, PALETTES.length)];
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float t = (x + y) / (float) (width + height);
                int r = (int) (palette[0].getRed() * (1 - t) + palette[1].getRed() * t);
                int g = (int) (palette[0].getGreen() * (1 - t) + palette[1].getGreen() * t);
                int b = (int) (palette[0].getBlue() * (1 - t) + palette[1].getBlue() * t);
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x222222));
        if (portrait) {
            g.fillOval(width / 3, height / 5, width / 3, width / 3);
            g.fillOval(width / 8, height / 2, width * 3 / 4, height);
        } else {
            g.fillOval(height / 8, height / 8, height * 3 / 4, height * 3 / 4);
            g.fillRect(height + 10, height / 3, width - height - 30, height / 3);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
