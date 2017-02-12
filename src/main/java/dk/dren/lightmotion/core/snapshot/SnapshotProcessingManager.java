package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.CameraManager;
import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventConsumer;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages snapshots from a single camera
 */
@Log
public class SnapshotProcessingManager {
    private final List<SnapshotProcessor> processors;
    @Getter
    private final File snapshotsDir;
    @Getter
    private final boolean storeSnapshots;
    @Getter
    private final LightMotionEventConsumer eventConsumer;
    @Getter
    private final String cameraName;
    @Getter
    private final File workingDir;

    public SnapshotProcessingManager(String cameraName, File workingDir, boolean storeSnapshots, LightMotionEventConsumer eventConsumer) {
        this.cameraName = cameraName;
        this.workingDir = workingDir;
        snapshotsDir = new File(workingDir, "snapshots");
        this.storeSnapshots = storeSnapshots;
        this.eventConsumer = eventConsumer;
        try {
            FileUtils.forceMkdir(workingDir);
            FileUtils.forceMkdir(snapshotsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dir for "+cameraName, e);
        }

        processors = new ArrayList<>();
        processors.add(new MotionDetector(this));
    }

    public SnapshotProcessingManager(CameraManager cm) {
        this(cm.getCameraConfig().getName(), cm.workingDir(), cm.getCameraConfig().getStoreSnapshots(), cm.getLightMotion());
    }

    public void processSnapshot(byte[] imageBytes) throws IOException {
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        final FixedPointPixels fixed = new FixedPointPixels(image);

        if (storeSnapshots) {
            fixed.write(new File(snapshotsDir, CameraManager.getTimeStamp() + ".png"));
        }

        for (SnapshotProcessor processor : processors) {
            try {
                LightMotionEvent event = processor.process(fixed);
                if (event != null) {
                    eventConsumer.consumeEvent(event);
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "An exception was thrown while processing image from "+cameraName, e);
                eventConsumer.consumeEvent(new LightMotionEvent(LightMotionEventType.FAILED_PROCESSOR, cameraName, "Exception while running "+processor.getClass().getSimpleName()+": "+e.toString()));
            }
        }
    }
}
