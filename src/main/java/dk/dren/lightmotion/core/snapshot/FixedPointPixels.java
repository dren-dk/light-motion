package dk.dren.lightmotion.core.snapshot;

import lombok.Getter;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
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
            long t0 = System.currentTimeMillis();

            BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics graphics = converted.getGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            image = converted;
            long duration = System.currentTimeMillis()-t0;
            log.warning("Converted image type "+image.getType()+" in "+duration+" ms");
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

    public static FixedPointPixels readFromAnyBytes(String name, byte[] imageBytes) throws IOException {
        if (imageBytes[0] == 'P' && imageBytes[1] == '6') {
            return PPMParser.readPPM6(name, imageBytes);
        } else {
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            return new FixedPointPixels(name, image);
        }
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

    public FixedPointPixels scale(int divisor) {
        FixedPointPixels img = new FixedPointPixels(name+"_"+divisor, width/divisor, height/divisor, false);

        int ytogo = divisor;
        int linestart = 0;
        int input = 0;
        for (int y=0;y<height;y++) {
            int xtogo = divisor;
            int output = linestart;
            for (int x = 0; x< this.width; x++) {
                img.pixels[output]   += pixels[input++] >> 16;
                img.pixels[output+1] += pixels[input++] >> 16;
                img.pixels[output+2] += pixels[input++] >> 16;

                if (--xtogo == 0) {
                    xtogo = divisor;
                    output += 3;
                }
            }

            if (--ytogo == 0) {
                ytogo = divisor;
                linestart += img.width*3;
            }
        }

        int inputPixelsPerOutputPixel = divisor*divisor;
        for (int i=0;i<img.pixels.length;i++) {
            int pixel = img.pixels[i] / inputPixelsPerOutputPixel;
            img.pixels[i] = pixel << 16;
        }

        return img;
    }
}
