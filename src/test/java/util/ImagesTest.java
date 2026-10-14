package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import testdata.Images;

/** The helpers of the golden tests must say where images differ, as that is what we look at when they fail */
public class ImagesTest {

    private static byte[] image(int width, int height, Color background, Color square, int squareX, int squareY) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        if (square != null) {
            g.setColor(square);
            g.fillRect(squareX, squareY, 20, 10);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    public void identicalImagesDoNotDiffer() throws Exception {
        byte[] a = image(100, 60, Color.WHITE, Color.RED, 10, 10);
        assertEquals(0, Images.difference(a, a));
        assertTrue(Images.describeDifferences(a, a).startsWith("no pixel differs"));
    }

    @Test
    public void saysWhereTheyDiffer() throws Exception {
        byte[] a = image(100, 60, Color.WHITE, null, 0, 0);
        byte[] b = image(100, 60, Color.WHITE, Color.BLACK, 30, 40);
        assertTrue(Images.difference(a, b) > 0);
        assertEquals("200 of 6000 pixels differ visibly (by more than 24), in x 30..49, y 40..49 of 100x60",
                Images.describeDifferences(a, b));
        // and the image of the differences only has them
        var differences = ImageIO.read(new ByteArrayInputStream(Images.differencesImage(a, b)));
        assertEquals(0xFFFFFF, differences.getRGB(35, 45) & 0xFFFFFF);
        assertEquals(0, differences.getRGB(5, 5) & 0xFFFFFF);
    }

    @Test
    public void imagesOfAnotherSizeDiffer() throws Exception {
        byte[] a = image(100, 60, Color.WHITE, null, 0, 0);
        byte[] b = image(120, 60, Color.WHITE, null, 0, 0);
        assertEquals(Double.POSITIVE_INFINITY, Images.difference(a, b));
        assertEquals("the size is 120x60 instead of 100x60", Images.describeDifferences(a, b));
    }
}
