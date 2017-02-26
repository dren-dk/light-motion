package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.events.LightMotionEvent;

/**
 * The interface the different snapshot processors must implement
 */
public interface SnapshotProcessor {
    LightMotionEvent process(FixedPointPixels image);
}
