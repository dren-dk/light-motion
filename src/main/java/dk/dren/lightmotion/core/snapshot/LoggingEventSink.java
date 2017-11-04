package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.db.entity.Event;
import dk.dren.lightmotion.core.events.LightMotionEventSink;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class LoggingEventSink implements LightMotionEventSink {

    @Getter
    @Setter
    private String prefix;

    @Override
    public void notify(Event event) {
        log.info(prefix+event.toString());
    }
}
