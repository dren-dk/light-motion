package dk.dren.lightmotion.core.snapshot;

import dk.dren.lightmotion.core.events.EventSinkWithMotionConfigOracle;
import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.entity.Event;
import dk.dren.lightmotion.core.events.EventSink;
import dk.dren.lightmotion.db.entity.MotionConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.sql.Timestamp;

@Log
public class TestEventSink implements EventSinkWithMotionConfigOracle {

    @Getter
    @Setter
    private String prefix;

    @Override
    public void notify(Event event) {
        log.info(prefix+event.toString());
    }

    @Override
    public MotionConfig getMotionConfig(Camera camera) {
        return new MotionConfig(0, new Timestamp(0), "Fake config", 40, 2,2);
    }
}
