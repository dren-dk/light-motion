package dk.dren.lightmotion.db.entity;

import dk.dren.lightmotion.core.events.LightMotionEventType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

/**
 * An event that has happend in the system
 */
@RequiredArgsConstructor
@Getter
public class Event {
    private final Long id;
    private final Timestamp timestamp;
    private final LightMotionEventType type;
    private final boolean canceling;
    private final long cameraId;
    private final String text;

    public static Event start(LightMotionEventType type, Camera camera, String text) {
        return new Event(null, new Timestamp(System.currentTimeMillis()), type, false, camera.getId(), text);
    }

    public static Event end(LightMotionEventType type, Camera camera, String text) {
        return new Event(null, new Timestamp(System.currentTimeMillis()), type, true, camera.getId(), text);
    }

    @Override
    public String toString() {
        return (canceling ? "!" : "") +  type + " from " + cameraId+": " + text;
    }
}
