package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.db.entity.Event;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Detects motion in the image
 */
@Log
public class MotionDetector implements SnapshotProcessor {
    public static final int STATE_STORAGE_INTERVAL = 30 * 1000;
    private final SnapshotProcessingManager manager;
    private final File averageFile;
    private final File debugDir;
    private final File maskFile;
    private final int threshold;
    private final int minArea;

    private FixedPointPixels average;
    private FixedPointPixels noise;
    private boolean quiet = true;
    int quietCount = 0;
    BitPixels currentMask;

    public MotionDetector(SnapshotProcessingManager manager) {
        this.manager = manager;
        averageFile = new File(manager.getStateDir(), "average.png");
        debugDir = System.getProperty("debug.dir", "").isEmpty() ? null : new File(System.getProperty("debug.dir"));
        maskFile = new File(manager.getStateDir(), "movement-mask.png");

        threshold = 40;
        minArea = 3;
    }

    private BitPixels loadCompatibleMask(FixedPointPixels snapshot) {
        if (currentMask != null && currentMask.isSameSizeAs(snapshot)) {
            return currentMask;
        }

        if (!maskFile.isFile()) {
            return null;
        }

        BufferedImage raw = null;
        try {
            raw = ImageIO.read(maskFile);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load mask for "+manager.getCamera().getName()+" from "+maskFile, e);
        }

        BufferedImage bi = new BufferedImage(snapshot.getWidth(), snapshot.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = bi.getGraphics();
        g.drawImage(raw, 0,0,
                bi.getWidth(), bi.getHeight(),
                new Color(0xff, 0xff, 0xff), null);
        g.dispose();

        currentMask = new BitPixels(bi);


        BufferedImage cb = currentMask.toBufferedImage();
        try {
            ImageIO.write(cb, "png", new File(maskFile.getParent(),"movement-mask-loaded.png"));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Fail", e);
        }

        return currentMask;
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
    private static FixedPointPixels motionDetect(FixedPointPixels average, FixedPointPixels other, BitPixels mask, int decayOrder, int diffWidth, int diffHeight) {
        if (average.getWidth() % diffWidth != 0) {
            throw new IllegalArgumentException("The diffWidth="+diffWidth+" must be a whole fraction of imageWidth="+average.getWidth());
        }
        if (average.getHeight() % diffHeight != 0) {
            throw new IllegalArgumentException("The diffHeight="+diffHeight+" must be a whole fraction of imageHeight="+average.getName());
        }

        // Number of pixels per diff pixel
        final int xpitch = 3*average.getWidth()/diffWidth;
        final int ypitch = average.getHeight()/diffHeight;

        final FixedPointPixels diffImage = new FixedPointPixels(other.getName()+"-diff", diffWidth, diffHeight, true);
        final int[] otherPixels = other.getPixels();
        final int[] diffPixels = diffImage.getPixels();
        final int inputSubWidth = average.getWidth() * 3;

        int firstOutputPixelInLine = 0;
        int inputIndex = 0;
        int maskPixel = 0;
        int ypixelsToGo = ypitch;
        int[] averagePixels = average.getPixels();
        for (int inputY = 0 ; inputY<average.getHeight() ; inputY++) {

            int outputPixel = firstOutputPixelInLine;
            int xpixelsToGo = xpitch;
            int maskPixelToGo = 3;
            for (int inputX = 0; inputX< inputSubWidth; inputX++) {
                int diff = otherPixels[inputIndex]-averagePixels[inputIndex];

                if (mask == null || !mask.isBlack(maskPixel)) {
                    diffPixels[outputPixel] += Math.abs(diff) >> 16;
                }
                if (--maskPixelToGo == 0) {
                    maskPixel++;
                    maskPixelToGo = 3;
                }

                averagePixels[inputIndex] += diff >> decayOrder;

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

        // Scale the diff pixels so we have a comparable scale
        final int inputPixelsPerOutputPixel = xpitch * ypitch;
        for (int diffPixel=0 ; diffPixel < diffPixels.length ; diffPixel++) {
            diffPixels[diffPixel] = (diffPixels[diffPixel] / inputPixelsPerOutputPixel) << 16;
        }

        return diffImage;
    }


    @Override
    public Event process(FixedPointPixels image) {
        FixedPointPixels blocky = image.scale(8);

        int imagePixelCount = blocky.getWidth() * blocky.getHeight();
        log.fine("Got image: "+blocky.getWidth()+"x"+blocky.getHeight()+" pixels: "+imagePixelCount+" sub-pixels: "+blocky.getPixels().length);

        if (average == null || average.getPixels().length != blocky.getPixels().length)  {
            average = blocky.clone(manager.getCamera().getName()+"-average");

        } else {

            long t0 = System.nanoTime();
//            long diff = average.diffBucketUpdate(image, 4);
            BitPixels mask = loadCompatibleMask(blocky);
            FixedPointPixels diff = motionDetect(average, blocky, mask, 4, blocky.getWidth()/2, blocky.getHeight()/2);
            long t1 = System.nanoTime();
            if (noise == null) {
                noise = diff.clone(manager.getCamera()+"-noise");
            }
            updateAverageAndSubtract(noise, diff, 4);


            log.fine(manager.getCamera()+": diff time: "+(t1-t0));

            storeState();

            MotionDetectionResult detected = analyzeDiff(diff, threshold);
            if (debugDir != null) {
                storeDebug(debugDir, average, image, diff, noise, detected);
            }

            if (detected.isMovementDetected())  {
                log.fine("Detected motion at "+detected.getMaxDiffX()+","+detected.getMaxDiffY()+ " = "+detected.getMaxDiff());
                quiet = false;
                quietCount = 0;
                return Event.start(LightMotionEventType.MOTION, manager.getCamera(), "Detected motion ("+detected.getMaxDiff()+")");
            } else {
                if (!quiet && quietCount++ > 10) {
                    quiet = true;
                    return Event.end(LightMotionEventType.MOTION, manager.getCamera(), "No motion detected");
                }
            }
        }

        return null;
    }

    private long lastStateStorage = 0;
    private void storeState() {

        long now = System.currentTimeMillis();
        if (now-lastStateStorage > STATE_STORAGE_INTERVAL) {
            lastStateStorage = now+(int)(Math.random()*10000); // Add a little randomness, to allow the stage storage of different motion detectors to be spread out in time
            try {
                average.write(averageFile);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to write average to " + averageFile, e);
            }
        }
    }

    private void updateAverageAndSubtract(FixedPointPixels noise, FixedPointPixels diff, final int decay) {
        int[] noisePixels = noise.getPixels();
        int[] diffPixels = diff.getPixels();

        for (int i=0;i<noisePixels.length;i++) {
            int diffPixel = diffPixels[i];
            int noisePixel = noisePixels[i];
            int adjustment = (diffPixel - noisePixel) >> decay;
            noisePixel += adjustment;

            if (noisePixel > diffPixel) {
                diffPixels[i] = 0;
            } else {
                diffPixels[i] = diffPixel - noisePixel;
            }
            noisePixels[i] = noisePixel;
        }
    }

    private void updateAverageAndSubtractBlurred(FixedPointPixels noise, FixedPointPixels diff, final int decay) {
        int[] noisePixels = noise.getPixels();
        int[] diffPixels = diff.getPixels();

        int w = noise.getWidth();
        int h = noise.getHeight();
        int lastPixelOnLine = w - 1;

        int pixel = 0;
        for (int y=0;y<noise.getHeight();y++) {
            for (int x=0;x<noise.getWidth();x++) {
                int diffPixel = diffPixels[pixel];
                int noisePixel = noisePixels[pixel];
                if (noisePixel < 0) {
                    noisePixel = 0;
                }
                int adjustment = (diffPixel - noisePixel) >> decay;
                noisePixel += adjustment;
                noisePixels[pixel] = noisePixel;
                if (noisePixel > diffPixel) {
                    diffPixels[pixel] = 0;
                } else {
                    diffPixels[pixel] = diffPixel - noisePixel;
                }

                int blurAdjustment = adjustment >> 2;
                if (blurAdjustment > 0) {
                    int cornerBlurAdjustment = blurAdjustment >> 1;
                    if (y != 0) {
                        if (x != 0) {
                            noisePixels[pixel - w - 1] += cornerBlurAdjustment;
                        }
                        noisePixels[pixel - w] += blurAdjustment;
                        if (x < lastPixelOnLine) {
                            noisePixels[pixel - w + 1] += cornerBlurAdjustment;
                        }
                    }

                    if (x != 0) {
                        noisePixels[pixel - 1] += blurAdjustment;
                    }
                    if (x < lastPixelOnLine) {
                        noisePixels[pixel + 1] += blurAdjustment;
                    }

                    if (y < h - 1) {
                        if (x != 0) {
                            noisePixels[pixel + w - 1] += cornerBlurAdjustment;
                        }
                        noisePixels[pixel + w] += blurAdjustment;
                        if (x < lastPixelOnLine) {
                            noisePixels[pixel + w + 1] += cornerBlurAdjustment;
                        }
                    }
                }

                pixel++;
            }
        }
    }

    private MotionDetectionResult analyzeDiff(FixedPointPixels diffImage, int threshold) {
        int[] diffPixels = diffImage.getPixels();
        // Find the diff pixel with the greatest difference
        int maxDiff = 0;
        int maxDiffPixel = -1;
        for (int diffPixel=0 ; diffPixel < diffPixels.length ; diffPixel++) {
            int diff = diffPixels[diffPixel];
            if (diff > maxDiff) {
                maxDiff = diff;
                maxDiffPixel = diffPixel;
            }
        }

        maxDiff >>= 16;

        return new MotionDetectionResult(maxDiff>=threshold, maxDiff, maxDiffPixel % diffImage.getWidth(), maxDiffPixel / diffImage.getWidth(), threshold);
    }

    private static void storeDebug(File debugDir, FixedPointPixels average, FixedPointPixels image, FixedPointPixels diffImage, FixedPointPixels noise, MotionDetectionResult diff) {
        BufferedImage debug = new BufferedImage(image.getWidth()*2, image.getHeight()*2, BufferedImage.TYPE_3BYTE_BGR);

        BufferedImage ai = average.toBufferedImage();
        BufferedImage ii = image.toBufferedImage();
        BufferedImage di = diffImage.toBufferedImageWithGradient(diff.getThreshold());
        BufferedImage ni = noise.toBufferedImageWithGradient(diff.getThreshold());

        Graphics graphics = debug.getGraphics();
        graphics.drawImage(ai, 0,               0,             image.getWidth(), image.getHeight(), null);
        graphics.drawImage(ii, image.getWidth(), 0,                image.getWidth(), image.getHeight(), null);
        graphics.drawImage(di, 0,               image.getHeight(), image.getWidth(), image.getHeight(), null);
        graphics.drawImage(ni, image.getWidth(), image.getHeight(),  image.getWidth(), image.getHeight(), null);
        if (diff.isMovementDetected()) {
            int xscale = image.getWidth() / diffImage.getWidth();
            int yscale = image.getHeight() / diffImage.getHeight();
            int x0 = image.getWidth() + xscale * diff.getMaxDiffX();
            int y0 = yscale * diff.getMaxDiffY();

            graphics.setColor(new Color(0xff, 0x00, 0x00));
            graphics.drawLine(x0, y0, x0+xscale-1, y0+yscale-1);
            graphics.drawLine(x0+xscale-1, y0, x0, y0+yscale-1);

            graphics.setColor(new Color(0xff, 0x90, 0x00));
            graphics.drawLine(x0, y0, x0+xscale-1, y0);
            graphics.drawLine(x0+xscale-1, y0+yscale-1, x0+xscale-1, y0);
            graphics.drawLine(x0+xscale-1, y0+yscale-1, x0, y0+yscale-1);
            graphics.drawLine(x0, y0, x0, y0+yscale-1);
        }

        graphics.dispose();

        File debugFile = new File(debugDir, "debug-" + image.getName() + ".png");
        try {
            FileUtils.forceMkdirParent(debugFile);
            ImageIO.write(debug, "png", debugFile);
        } catch (IOException e) {
            throw new RuntimeException("Fail!");
        }
    }
}
