package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

/**
 * An image consisting of fixed-point sub-pixels.
 *
 * This means that each sub-pixel is either red green or blue.
 *
 * each pixel is an integer.
 *
 *
 */
@Log
@Getter
public class FixedPointPixels {
    private final int[] pixels;
    private final int width;
    private final int height;
    private final int imageType;

    public FixedPointPixels(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        imageType = image.getType();
        final byte[] inputPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        pixels = new int[inputPixels.length];

        long t0 = System.nanoTime();
        for (int i=0;i<inputPixels.length;i++) {
            pixels[i] = ((int)(inputPixels[i]) & 0xff) << 16;
        }
        long duration = System.nanoTime()-t0;
        log.fine("Converted "+pixels.length+" pixels in "+duration+" ns");
    }

    public long diffBucketSum(FixedPointPixels other) {

        final int[] otherPixels = other.getPixels();
        final int subWidth = width*3;
        final int xbp = subWidth / 16;

        final int ybp = height / 16;

        final long[] buckets = new long[256];
        int xi = 0;
        int yi = 0;
        for (int i=0;i<pixels.length;i++) {
            int xb = xi/xbp;
            int yb = yi/ybp;
            buckets[xb + (yb<<4)] += Math.abs(this.pixels[i]- otherPixels[i]) >> 16;

            if (++xi >= subWidth) {
                yi++;
                xi = 0;
            }
        }

        long result = 0;
        for (long bs : buckets) {
            if (bs > result) {
                result = bs;
            }
        }

        return result / (xbp*ybp);
    }

    public long diffSum(FixedPointPixels other) {

        int[] otherPixels = other.getPixels();
        long result = 0;
        for (int i=0;i<pixels.length;i++) {
            result += Math.abs(this.pixels[i]- otherPixels[i]) >> 16;
        }

        return result / pixels.length;
    }

    /**
     * Calculates the difference (0..255) between the new image and this one, while updating the current image with a
     * bit of the image, so the current image ends up being a moving average of the images that are diffed against it.
     *
     * @param other The new image to diff against and add to the average
     * @param decayOrder the number of bits to shift the diff when updating average.
     * @return The average difference across all pixels in the image
     */
    public long diffAndUpdate(FixedPointPixels other, int decayOrder) {

        int[] otherPixels = other.getPixels();
        long result = 0;
        for (int i=0;i<pixels.length;i++) {
            int diff = otherPixels[i]-this.pixels[i];

            result += Math.abs(diff);

            this.pixels[i] += diff >> decayOrder;
        }

        return (result / pixels.length) >> 16;
    }

    BufferedImage toBufferedImage() {
        BufferedImage bi = new BufferedImage(width, height, imageType);
        final byte[] outputPixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();

        for (int i=0;i<pixels.length;i++) {
            outputPixels[i] = (byte)(pixels[i] >> 16);
        }

        return bi;
    }

    public void write(File file) throws IOException {
        BufferedImage bi = toBufferedImage();
        ImageIO.write(bi, "png", file);
    }

    public static FixedPointPixels read(File file) throws IOException {
        return new FixedPointPixels(ImageIO.read(file));
    }
}
