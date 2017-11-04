package dk.dren.lightmotion.core.events;

import dk.dren.lightmotion.db.entity.Event;

/**
 *
 */
public interface LightMotionEventSink {
    void notify(Event event);
}
