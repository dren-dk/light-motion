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
    private final LightMotionEventType type;
    private final String cameraName;
    private final String text;

    public LightMotionEvent(LightMotionEventType type, CameraManager cameraManager, String text) {
        this(type, cameraManager.getCameraConfig().getName(), text);
    }

    @Override
    public String toString() {
        return type + " from " + cameraName+": " + text;
    }
}
