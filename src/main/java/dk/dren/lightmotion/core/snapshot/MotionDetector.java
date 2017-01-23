package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.CameraManager;
import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventConsumer;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.extern.java.Log;

import java.io.File;
import java.util.logging.Level;

/**
 * Detects motion in the image
 */
@Log
public class MotionDetector implements SnapshotProcessor {
    final private SnapshotProcessingManager manager;
    private FixedPointPixels average;
    int quietCount = 0;
    private boolean quiet = true;
    private File averageFile;

    public MotionDetector(SnapshotProcessingManager manager) {
        this.manager = manager;
        averageFile = new File(manager.getWorkingDir(), "average.png");
    }

    @Override
    public LightMotionEvent process(FixedPointPixels image) {

        int imagePixelCount = image.getWidth() * image.getHeight();
        log.fine("Got image: "+image.getWidth()+"x"+image.getHeight()+" pixels: "+imagePixelCount+" sub-pixels: "+image.getPixels().length);

        if (average == null)  {
            average = image;

        } else if (average.getPixels().length != image.getPixels().length){
            // Note: I don't know if this is a reasonable thing to happen, if it is then this code should learn how to handle it rather than throw an exception
            throw new RuntimeException("The number of pixels of the new image "+image.getPixels().length+" is different from the previously seen "+average.getPixels().length);

        } else {

            long t0 = System.nanoTime();
            long diff = average.diffAndUpdate(image, 4);
            long t1 = System.nanoTime();
            log.fine(manager.getCameraName()+": diff time: "+(t1-t0)+" diff="+diff);

            if (diff > 10)  {
                log.info("Detected motion");
                quiet = false;
                quietCount = 0;
                return new LightMotionEvent(LightMotionEventType.MOTION, manager.getCameraName(), "Detected motion ("+diff+")");
            } else {
                if (!quiet && quietCount++ > 10) {
                    quiet = true;
                    return new LightMotionEvent(LightMotionEventType.QUIET, manager.getCameraName(), "No motion detected");
                }
            }

            try {
                average.write(averageFile);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to write average to "+averageFile, e);
            }
        }

        return null;
    }
}
