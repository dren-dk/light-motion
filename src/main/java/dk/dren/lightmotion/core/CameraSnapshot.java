package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.snapshot.SnapshotProcessingManager;

import java.io.IOException;

public interface CameraSnapshot {
    SnapshotProcessingManager getSnapshotProcessingManager();

    String getName();

    byte[] getImageBytes();

    void processSnapshot() throws IOException;
}
