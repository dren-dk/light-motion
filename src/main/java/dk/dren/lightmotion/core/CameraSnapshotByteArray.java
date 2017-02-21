package dk.dren.lightmotion.core;

import dk.dren.lightmotion.core.snapshot.SnapshotProcessingManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * An image retrieved from a camera, note that as much work as possible must be done in the receiving thread,
 * not in the constructor, because the constructor could end up running in many more threads than there are CPUs
 * available.
 */
@RequiredArgsConstructor
@Getter
public class CameraSnapshotByteArray implements CameraSnapshot {
    /**
     * The processing manager that needs to handle this snapshot
     */
    private final SnapshotProcessingManager snapshotProcessingManager;

    /**
     * The name of the snapshot, should be globally unique, as it's nice to have for debugging and user freedback
     */
    private final String name;

    /**
     * The actual bytes of the image
     */
    private final byte[] imageBytes;

    public void processSnapshot() throws IOException {
        snapshotProcessingManager.processSnapshot(name, imageBytes);
    }
}
