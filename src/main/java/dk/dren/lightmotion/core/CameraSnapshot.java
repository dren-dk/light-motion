package dk.dren.lightmotion.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An image retrieved from a camera, note that as much work as possible must be done in the receiving thread,
 * not in the constructor, because the constructor could end up running in many more threads than there are CPUs
 * available.
 */
@RequiredArgsConstructor
@Getter
public class CameraSnapshot {
    private final CameraManager source;
    private final byte[] imageBytes;

    public void processSnapshot() {
        source.processSnapshot(imageBytes);
    }
}
