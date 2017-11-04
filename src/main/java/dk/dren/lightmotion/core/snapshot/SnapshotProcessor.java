package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.db.entity.Event;

/**
 * The interface the different snapshot processors must implement
 */
public interface SnapshotProcessor {
    Event process(FixedPointPixels image);
}
