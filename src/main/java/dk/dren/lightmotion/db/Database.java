package dk.dren.lightmotion.db;

import dk.dren.lightmotion.core.events.LightMotionEventType;
import dk.dren.lightmotion.db.entity.Camera;
import dk.dren.lightmotion.db.entity.Event;
import dk.dren.lightmotion.db.entity.MotionConfig;
import dk.dren.lightmotion.db.mapper.CameraMapper;
import dk.dren.lightmotion.db.mapper.EventMapper;
import dk.dren.lightmotion.db.mapper.MotionConfigMapper;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

/**
 * The great big DAO used for accessing the database
 */
@RegisterMapper({CameraMapper.class, EventMapper.class, MotionConfigMapper.class})
public interface Database {

    // ---------------------------------------------------------------------------------------------------------------
    // Camera

    @SqlQuery(CameraMapper.SQL+"order by name")
    List<Camera> getAllCameras();

    @SqlQuery(CameraMapper.SQL+"where name=:name")
    Camera getCameraByName(@Bind("name") String name);

    @SqlUpdate("insert into camera (name, address, onvifUser, onvifPassword, profileNumber, lowResProfileNumber, lowResSnapshot) " +
            "values (:name, :address, :user, :password, :profileNumber, :lowResProfileNumber, :lowResSnapshot)")
    @GetGeneratedKeys
    long insertCamera(@BindBean Camera camera);

    @SqlUpdate("update camera set address=:address, onvifuser=:user, onvifpassword=:password, profileNumber=:profileNumber," +
            " lowResProfileNumber=:lowResProfileNumber, lowResSnapshot=:lowResSnapshot")
    void updateCameraByName(@BindBean Camera camera);


    // ---------------------------------------------------------------------------------------------------------------
    // Event

    @SqlUpdate("insert into event (type, canceling, camera_id, text) values (:type, :canceling, :cameraId, :text)")
    @GetGeneratedKeys
    long insertEvent(@BindBean Event event);


    // ---------------------------------------------------------------------------------------------------------------
    // MotionConfig

    @SqlQuery(MotionConfigMapper.SQL)
    List<MotionConfig> getAllMotionConfigs();

    @SqlUpdate("insert into motion_config (name, motion_threshold, chunks_before_event, chunks_after_event) " +
            "values ('Default Motion Config', 40, 2, 2)")
    @GetGeneratedKeys
    int createDefaultMotionConfig();

    @SqlQuery(MotionConfigMapper.SQL+"where id=:id")
    MotionConfig getMotionConfig(@Bind("id")int id);
}
