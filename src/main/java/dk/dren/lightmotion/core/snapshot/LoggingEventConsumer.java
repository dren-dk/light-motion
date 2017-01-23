package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.events.LightMotionEvent;
import dk.dren.lightmotion.core.events.LightMotionEventConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
public class LoggingEventConsumer implements LightMotionEventConsumer {

    @Getter
    @Setter
    private String prefix;

    @Override
    public void consumeEvent(LightMotionEvent event) {
        log.info(prefix+event.toString());
    }
}
