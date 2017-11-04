package dk.dren.lightmotion.core.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An event that has happend in the system
 */
@RequiredArgsConstructor
@Getter
public class LightMotionEvent {
    private final long timestamp;
    private final LightMotionEventType type;
    private final boolean canceling;
    private final String cameraName;
    private final String text;

    public static LightMotionEvent start(LightMotionEventType type, String cameraName, String text) {
        return new LightMotionEvent(System.currentTimeMillis(), type, false, cameraName, text);
    }

    public static LightMotionEvent end(LightMotionEventType type, String cameraName, String text) {
        return new LightMotionEvent(System.currentTimeMillis(), type, true, cameraName, text);
    }

    @Override
    public String toString() {
        return (canceling ? "!" : "") +  type + " from " + cameraName+": " + text;
    }
}
