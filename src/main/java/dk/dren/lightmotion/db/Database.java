package dk.dren.lightmotion.db;

import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.mapper.CameraMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

/**
 * The great big DAO used for accessing the database
 */
@RegisterMapper(CameraMapper.class)
public interface Database {

    @SqlQuery(CameraMapper.SQL+"order by name")
    List<Camera> getAllCameras();

    @SqlQuery(CameraMapper.SQL+"where name=:name")
    Camera getCameraByName(@Bind("name") String name);

    @SqlUpdate("insert into camera (name, address, onvifUser, onvifPassword, profileNumber, lowResProfileNumber, lowResSnapshot) " +
            "values (:name, :address, :user, :password, :profileNumber, :lowResProfileNumber, :lowResSnapshot)")
    void insertCamera(@BindBean Camera camera);

    @SqlUpdate("update camera set address=:address, onvifuser=:user, onvifpassword=:password, profileNumber=:profileNumber," +
            " lowResProfileNumber=:lowResProfileNumber, lowResSnapshot=:lowResSnapshot")
    void updateCameraByName(@BindBean Camera camera);

}
