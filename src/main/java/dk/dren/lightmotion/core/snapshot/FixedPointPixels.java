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
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;

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
    private final boolean monochrome;
    private final String name;

    public FixedPointPixels(String name, BufferedImage image) {
        this.name = name;
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

        monochrome = false;

        final byte[] inputPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        pixels = new int[inputPixels.length]; // Note: 3 sub-pixels per input pixel

        long t0 = System.nanoTime();
        for (int i=0;i<inputPixels.length;i++) {
            pixels[i] = ((int)(inputPixels[i]) & 0xff) << 16;
        }
        long duration = System.nanoTime()-t0;
        log.fine("Converted "+pixels.length+" pixels in "+duration+" ns");
    }

    public FixedPointPixels(String name, int width, int height, boolean monochrome) {
        this.name = name;
        this.monochrome = monochrome;
        this.width = width;
        this.height = height;
        this.pixels = new int[monochrome ? width*height : width*height*3];
    }

    public FixedPointPixels(String name, FixedPointPixels original) {
        this.name = name;
        this.pixels = original.getPixels().clone();
        this.width = original.width;
        this.height = original.height;
        this.monochrome = original.monochrome;
    }

    /**
     * This method iterates through the input pixels once and in that pass it does two things:
     *
     * * Add the new image to the current image via a moving average algorithm.
     * * Generate a diff at low resolution (diffWidth*diffHeigh).
     * @param other The new image to add to the moving average and detect differences in
     * @param mask
     * @param decayOrder The order of decay to use for the moving average (4 means than 1/16 of the diff will be used to update the average)
     * @param diffWidth Width of the diff image
     * @param diffHeight Height of the diff image
     */
    public MotionDetectionResult motionDetect(FixedPointPixels other, BitPixels mask, int decayOrder, int diffWidth, int diffHeight) {
        if (width % diffWidth != 0) {
            throw new IllegalArgumentException("The diffWidth="+diffWidth+" must be a whole fraction of imageWidth="+width);
        }
        if (height % diffHeight != 0) {
            throw new IllegalArgumentException("The diffHeight="+diffHeight+" must be a whole fraction of imageHeight="+height);
        }

        // Number of pixels per diff pixel
        final int xpitch = 3*width/diffWidth;
        final int ypitch = height/diffHeight;

        final FixedPointPixels diffImage = new FixedPointPixels(other.getName()+"-diff", diffWidth, diffHeight, true);
        final int[] otherPixels = other.getPixels();
        final int[] diffPixels = diffImage.getPixels();
        final int inputSubWidth = width * 3;

        int firstOutputPixelInLine = 0;
        int inputIndex = 0;
        int maskPixel = 0;
        int ypixelsToGo = ypitch;
        for (int inputY = 0 ; inputY<height ; inputY++) {

            int outputPixel = firstOutputPixelInLine;
            int xpixelsToGo = xpitch;
            int maskPixelToGo = 3;
            for (int inputX = 0; inputX< inputSubWidth; inputX++) {
                int diff = otherPixels[inputIndex]-this.pixels[inputIndex];

                if (mask == null || !mask.isBlack(maskPixel)) {
                    diffPixels[outputPixel] += Math.abs(diff) >> 16;
                } else {
                    diffPixels[outputPixel] = 255;
                }
                if (--maskPixelToGo == 0) {
                    maskPixel++;
                    maskPixelToGo = 3;
                }

                this.pixels[inputIndex] += diff >> decayOrder;

                inputIndex++;

                if (--xpixelsToGo == 0) {
                    xpixelsToGo = xpitch;
                    outputPixel++;
                }
            }

            if (--ypixelsToGo == 0) {
                ypixelsToGo = ypitch;
                firstOutputPixelInLine += diffWidth;
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

        return new MotionDetectionResult(diffImage, maxDiff/ inputPixelsPerOutputPixel, maxDiffPixel % diffWidth, maxDiffPixel / diffWidth);
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

        BufferedImage bi = new BufferedImage(width, height, monochrome ? TYPE_BYTE_GRAY : TYPE_3BYTE_BGR);
        final byte[] outputPixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            outputPixels[i] = (byte) (pixels[i] >> 16);
        }
        return bi;
    }

    public void write(File file) throws IOException {
        BufferedImage bi = toBufferedImage();
        ImageIO.write(bi, "png", file);
    }

    public static FixedPointPixels read(File file) throws IOException {
        return new FixedPointPixels(file.getName(), ImageIO.read(file));
    }

    public BufferedImage toBufferedImageWithGradient(int threshold) {

        if (!monochrome) {
            throw new IllegalArgumentException("Can only turn a monocrhome image into a gradient");
        }

        BufferedImage bi = new BufferedImage(width, height, TYPE_3BYTE_BGR);
        final byte[] outputPixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();

        int outputPixel = 0;
        for (int i = 0; i < pixels.length; i++) {

            int grey = pixels[i] >> 16;

            int red = 0;
            int green = 0;
            int blue = 0;

            if (grey < threshold) {
                red = green = 255* grey / threshold;
            } else {
                red = 255;
                green = blue = 255*(grey-threshold)/(255-threshold);
            }

            outputPixels[outputPixel++] = (byte) (blue);
            outputPixels[outputPixel++] = (byte) (green);
            outputPixels[outputPixel++] = (byte) (red);
        }
        return bi;
    }

    public FixedPointPixels clone(String name) {
        return new FixedPointPixels(name, this);
    }
}
