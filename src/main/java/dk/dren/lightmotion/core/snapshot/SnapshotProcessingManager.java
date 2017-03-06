package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.CameraManager;
import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventSink;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
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
    private File stateDir;
    @Getter
    private final File preRecordDir;
    @Getter
    private final File recordingDir;
    @Getter
    private final LightMotionEventSink eventConsumer;
    @Getter
    private final String cameraName;
    @Getter
    private final File workingDir;

    public SnapshotProcessingManager(String cameraName, File workingDir, File stateDir, File preRecordDir, File recordingDir, boolean storeSnapshots, LightMotionEventSink eventConsumer) {
        this.cameraName = cameraName;
        this.workingDir = workingDir;
        snapshotsDir = storeSnapshots ? new File(preRecordDir, "snapshots") : null;
        this.stateDir = stateDir;
        this.preRecordDir = preRecordDir;
        this.recordingDir = recordingDir;
        this.eventConsumer = eventConsumer;
        try {
            FileUtils.forceMkdir(workingDir);
            if (snapshotsDir != null) {
                FileUtils.forceMkdir(snapshotsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dir for "+cameraName, e);
        }

        processors = new ArrayList<>();
        processors.add(new MotionDetector(this));
    }

    public SnapshotProcessingManager(CameraManager cm) {
        this(cm.getCameraConfig().getName(), cm.getWorkingDir(), cm.getStateDir(), cm.getChunkDir(), cm.getRecordingDir(), cm.getCameraConfig().getStoreSnapshots(), cm);
    }

    public void processSnapshot(String name, byte[] imageBytes) throws IOException {
        if (snapshotsDir != null) {
            String mimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(imageBytes));
            String type = "ppm";
            if (mimeType != null && mimeType.startsWith("image/")) {
                type = mimeType.replaceAll("image/", "");
            }
            FileUtils.writeByteArrayToFile(new File(snapshotsDir, name + "." + type), imageBytes);
        }


        long t0 = System.currentTimeMillis();
        final FixedPointPixels fixed = FixedPointPixels.readFromAnyBytes(name, imageBytes);
        long duration = System.currentTimeMillis()-t0;
        if (duration > 100) {
            log.warning("Loaded " + name + " in " + duration + " ms");
        }

        for (SnapshotProcessor processor : processors) {
            try {
                LightMotionEvent event = processor.process(fixed);
                if (event != null) {
                    eventConsumer.notify(event);
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "An exception was thrown while processing image from "+cameraName, e);
                eventConsumer.notify(LightMotionEvent.start(LightMotionEventType.FAILED_PROCESSOR, cameraName, "Exception while running "+processor.getClass().getSimpleName()+": "+e.toString()));
            }
        }
    }
}
