package dk.dren.lightmotion.db.mapper;

import dk.dren.lightmotion.core.events.LightMotionEventType;
import dk.dren.lightmotion.db.entity.Event;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Turns a row from the event table into an Event object
 */
public class EventMapper implements ResultSetMapper<Event> {
    public static final String SQL = "select id, created, type, canceling, camera_id, text FROM event ";

    @Override
    public Event map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Event(
                resultSet.getLong("id"),
                resultSet.getTimestamp("created"),
                LightMotionEventType.valueOf(resultSet.getString("type")),
                resultSet.getBoolean("canceling"),
                resultSet.getLong("camera_id"),
                resultSet.getString("text")
                );
    }
}
