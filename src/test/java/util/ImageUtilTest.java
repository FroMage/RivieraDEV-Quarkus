package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

public class ImageUtilTest {

    @Test
    public void largeLandscapeImageIsScaledByWidth() {
        BufferedImage scaled = ImageUtil.scaleImage(new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB), 400);
        assertEquals(400, scaled.getWidth());
        assertEquals(200, scaled.getHeight());
    }

    @Test
    public void largePortraitImageIsScaledByHeight() {
        BufferedImage scaled = ImageUtil.scaleImage(new BufferedImage(500, 1000, BufferedImage.TYPE_INT_RGB), 400);
        assertEquals(200, scaled.getWidth());
        assertEquals(400, scaled.getHeight());
    }

    @Test
    public void smallImageIsNotScaled() {
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        assertSame(image, ImageUtil.scaleImage(image, 400));
    }

    @Test
    public void imageWithAlphaIsConvertedToRgbEvenWhenNotScaled() {
        // we write JPEGs, which fail with an alpha channel
        BufferedImage scaled = ImageUtil.scaleImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB), 400);
        assertEquals(BufferedImage.TYPE_INT_RGB, scaled.getType());
        assertEquals(100, scaled.getWidth());
    }

    @Test
    public void writesAJpeg() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageUtil.writeImage(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), out);
        byte[] bytes = out.toByteArray();
        // JPEG start of image marker
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xD8, bytes[1]);
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(read);
        assertEquals(40, read.getWidth());
        assertEquals(30, read.getHeight());
    }
}
