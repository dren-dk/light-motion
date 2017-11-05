package dk.dren.lightmotion.db.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

/**
 * A motion configuration from the database
 */
@RequiredArgsConstructor
@Getter
public class MotionConfig {
    private final int id;
    private final Timestamp created;
    private final String name;
    private final int motionThreshold;
    private final int chunksBeforeEvent;
    private final int chunksAfterEvent;
}
