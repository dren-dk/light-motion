package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 * An image consisting of fixed-point sub-pixels.
 *
 * This means that each sub-pixel is either red green or blue.
 *
 * each sub-pixel is an integer scaled by 1<<16.
 *
 * The sub-pixel order is BGR
 */
@Log
@Getter
public class FixedPointPixels {
    private final int[] pixels;
    private final int width;
    private final int height;

    public FixedPointPixels(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();

        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            log.warning("Converting image type "+image.getType());
            BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics graphics = converted.getGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = converted;
        }

        final byte[] inputPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        pixels = new int[inputPixels.length]; // Note: 3 sub-pixels per input pixel

        long t0 = System.nanoTime();
        for (int i=0;i<inputPixels.length;i++) {
            pixels[i] = ((int)(inputPixels[i]) & 0xff) << 16;
        }
        long duration = System.nanoTime()-t0;
        log.fine("Converted "+pixels.length+" pixels in "+duration+" ns");
    }

    public FixedPointPixels(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width*height*3];
    }

    /**
     * This method iterates through the input pixels once and in that pass it does two things:
     *
     * * Add the new image to the current image via a moving average algorithm.
     * * Generate a diff at low resolution (diffWidth*diffHeigh).
     *  @param other The new image to add to the moving average and detect differences in
     * @param decayOrder The order of decay to use for the moving average (4 means than 1/16 of the diff will be used to update the average)
     * @param diffWidth Width of the diff image
     * @param diffHeight Height of the diff image
     */
    public MotionDetectionResult motionDetect(FixedPointPixels other, int decayOrder, int diffWidth, int diffHeight) {
        if (width % diffWidth != 0) {
            throw new IllegalArgumentException("The diffWidth="+diffWidth+" must be a whole fraction of imageWidth="+width);
        }
        if (height % diffHeight != 0) {
            throw new IllegalArgumentException("The diffHeight="+diffHeight+" must be a whole fraction of imageHeight="+height);
        }

        // Number of pixels per diff pixel
        final int xpitch = width/diffWidth;
        final int ypitch = height/diffHeight;

        final FixedPointPixels diffImage = new FixedPointPixels(diffWidth, diffHeight);
        final int[] otherPixels = other.getPixels();
        final int[] diffPixels = diffImage.getPixels();
        final int inputSubWidth = width * 3;
        final int diffSubWidth = diffWidth * 3;

        int firstOutputPixelInLine = 0;
        int inputIndex = 0;
        int ypixelsToGo = ypitch;
        for (int inputY = 0 ; inputY<height ; inputY++) {

            int outputPixel = firstOutputPixelInLine;
            int xpixelsToGo = xpitch;
            for (int inputX = 0; inputX< inputSubWidth; inputX++) {
                int diff = otherPixels[inputIndex]-this.pixels[inputIndex];

                diffPixels[outputPixel] += Math.abs(diff) >> 16;

                this.pixels[inputIndex] += diff >> decayOrder;
                inputIndex++;

                if (--xpixelsToGo == 0) {
                    xpixelsToGo = xpitch;
                    outputPixel++;
                }
            }

            if (--ypixelsToGo == 0) {
                ypixelsToGo = ypitch;
                firstOutputPixelInLine += diffSubWidth;
            }
        }

        // Find the diff pixel with the greatest difference
        int maxDiff = 0;
        int maxDiffPixel = -1;
        final int inputPixelsPerOutputPixel = xpitch * ypitch;
        for (int diffPixel=0 ; diffPixel < diffPixels.length ; diffPixel++) {
            int diff = diffPixels[diffPixel];
            if (diff > maxDiff) {
                maxDiff = diff;
                maxDiffPixel = diffPixel;
            }

            diffPixels[diffPixel] = (diff / inputPixelsPerOutputPixel) << 16;
        }

        int maxDiffFullPixel = maxDiffPixel/3;
        return new MotionDetectionResult(diffImage, maxDiff/ inputPixelsPerOutputPixel, maxDiffFullPixel % diffWidth, maxDiffFullPixel / diffWidth);
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
        BufferedImage bi = new BufferedImage(width, height, TYPE_3BYTE_BGR);
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
