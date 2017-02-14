package dk.dren.lightmotion.core.snapshot;

import lombok.extern.java.Log;
import org.roaringbitmap.RoaringBitmap;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

/**
 * A very, very efficient way to store images with one bit per pixel
 */
@Log
public class BitPixels {
    private final RoaringBitmap bitmap;
    private final int width;
    private final int height;

    public BitPixels(BufferedImage bi) {
        if (bi.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            throw new IllegalArgumentException("Cannot convert anything other than BYTE_BINARY images to bitpixels");
        }

        this.width = bi.getWidth();
        this.height = bi.getHeight();
        bitmap = new RoaringBitmap();
        final byte[] pixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        for (int i=0;i<pixels.length;i++) {
            if (pixels[i] == 0) {
                bitmap.add(i);
            }
            /*
            System.out.print(pixels[i]);
            if (i % 100 == 0) {
                System.out.println();
            }
            */
        }
    }

    public boolean isSameSizeAs(FixedPointPixels image) {
        return image.getWidth()==width && image.getHeight()==height;
    }

    public boolean isBlack(int bit) {
        return bitmap.contains(bit);
    }

    BufferedImage toBufferedImage() {

        BufferedImage bi = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        final byte[] outputPixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < outputPixels.length; i++) {
            outputPixels[i] = isBlack(i) ? (byte)0 : (byte)0xff;
        }
        return bi;
    }

}
