package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.CameraManager;
import dk.dren.lightmotion.core.events.EventSinkWithMotionConfigOracle;
import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.entity.Event;
import dk.dren.lightmotion.core.events.LightMotionEventType;
import dk.dren.lightmotion.db.entity.MotionConfig;
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
    private final Camera camera;
    @Getter
    private File stateDir;
    @Getter
    private final File preRecordDir;
    @Getter
    private final EventSinkWithMotionConfigOracle owner;
    @Getter
    private final File workingDir;

    public SnapshotProcessingManager(Camera camera, File workingDir, File stateDir, File preRecordDir, boolean storeSnapshots, EventSinkWithMotionConfigOracle owner) {
        this.camera = camera;
        this.workingDir = workingDir;
        snapshotsDir = storeSnapshots ? new File(preRecordDir, "snapshots") : null;
        this.stateDir = stateDir;
        this.preRecordDir = preRecordDir;
        this.owner = owner;
        try {
            FileUtils.forceMkdir(workingDir);
            if (snapshotsDir != null) {
                FileUtils.forceMkdir(snapshotsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dir for "+camera.getName(), e);
        }

        processors = new ArrayList<>();
        processors.add(new MotionDetector(this));
    }

    public SnapshotProcessingManager(CameraManager cm) {
        this(cm.getCamera(), cm.getWorkingDir(), cm.getStateDir(), cm.getChunkDir(), cm.getCamera().isLowResSnapshot(), new EventSinkWithMotionConfigOracle() {
            @Override
            public MotionConfig getMotionConfig(Camera camera) {
                return cm.getLightMotion().getMotionConfig(camera);
            }

            @Override
            public void notify(Event event) {
                cm.notify(event);
            }
        });
    }

    public MotionConfig getMotionConfig() {
        return owner.getMotionConfig(camera);
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
                Event event = processor.process(fixed);
                if (event != null) {
                    owner.notify(event);
                }

            } catch (Exception e) {
                log.log(Level.SEVERE, "An exception was thrown while processing image from "+camera.getName(), e);
                owner.notify(Event.start(LightMotionEventType.FAILED_PROCESSOR, camera, "Exception while running "+processor.getClass().getSimpleName()+": "+e.toString()));
            }
        }
    }
}
