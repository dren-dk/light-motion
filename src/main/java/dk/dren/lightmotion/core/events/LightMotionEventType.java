package dk.dren.lightmotion.core.events;

/**
 * A type of event in the lightmotion system
 */
public enum LightMotionEventType {
    // Failure to communicate
    FAILED_ONVIF,
    FAILED_STREAM,
    FAILED_SNAPSHOT,

    // Internal processor failure (exception thrown)
    FAILED_PROCESSOR,

    // Events from processors
    MOTION,
    WHITEOUT,
    BLACKOUT,
    GLOBAL_CHANGE,

    ;



    public boolean isDetection() {
        return this.equals(MOTION) || this.equals(WHITEOUT) || this.equals(BLACKOUT) || this.equals(GLOBAL_CHANGE);
    }
}
