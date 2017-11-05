package dk.dren.lightmotion.db.mapper;

import dk.dren.lightmotion.db.entity.Camera;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Turns a camera row into a Camera object
 */
public class CameraMapper implements ResultSetMapper<Camera> {
    public static final String SQL = "SELECT id, created, name, address, onvifuser, onvifpassword, profileNumber, lowresProfileNumber, lowresSnapshot, motion_config_id " +
            "FROM camera ";

    @Override
    public Camera map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Camera(resultSet.getLong("id"),
                resultSet.getTimestamp("created"),
                resultSet.getString("name"),
                resultSet.getString("address"),
                resultSet.getString("onvifuser"),
                resultSet.getString("onvifpassword"),
                resultSet.getInt("profileNumber"),
                resultSet.getInt("lowresprofilenumber"),
                resultSet.getBoolean("lowressnapshot"),
                (Integer)resultSet.getObject("motion_config_id")
        );
    }
}
