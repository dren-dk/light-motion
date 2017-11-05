package dk.dren.lightmotion.core.events;

import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.entity.MotionConfig;

public interface EventSinkWithMotionConfigOracle extends EventSink {
    MotionConfig getMotionConfig(Camera camera);
}
