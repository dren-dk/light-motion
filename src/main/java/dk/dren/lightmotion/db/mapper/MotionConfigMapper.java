package dk.dren.lightmotion.db.mapper;

import dk.dren.lightmotion.db.entity.MotionConfig;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Turns a row from the motion_config table into a MotionConfig object
 */
public class MotionConfigMapper implements ResultSetMapper<MotionConfig> {
    public static final String SQL = "select id, created, name, motion_threshold, chunks_before_event, chunks_after_event from motion_config ";

    @Override
    public MotionConfig map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new MotionConfig(
                resultSet.getInt("id"),
                resultSet.getTimestamp("created"),
                resultSet.getString("name"),
                resultSet.getInt("motion_threshold"),
                resultSet.getInt("chunks_before_event"),
                resultSet.getInt("chunks_after_event")
        );
    }
}
