package dk.dren.lightmotion.core.events;

import dk.dren.lightmotion.core.CameraManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An event that has happend in the system
 */
@RequiredArgsConstructor
@Getter
public class LightMotionEvent {
    private final long timestamp = System.currentTimeMillis();

    private final LightMotionEventType type;
    private final boolean canceling;
    private final String cameraName;
    private final String text;

    public LightMotionEvent(LightMotionEventType type, boolean canceling, CameraManager cameraManager, String text) {
        this(type, canceling, cameraManager.getCameraConfig().getName(), text);
    }

    @Override
    public String toString() {
        return (canceling ? "!" : "") +  type + " from " + cameraName+": " + text;
    }
}
