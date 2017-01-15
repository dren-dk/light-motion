package dk.dren.lightmotion.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * A camera to stream from.
 */
@Data
public class CameraConfig {

    /**
     * The address of the camera
     */
    @NotEmpty
    @JsonProperty
    String address;

    /**
     * The human-readable name/location of the camera, if left empty the address is used
     */
    @JsonProperty
    String name;

    /**
     * The RTSP port, if different from 554
     */
    @JsonProperty
    Integer rtspPort = 554;

    /**
     * The RTSP path of the camera, if different from /11
     */
    @JsonProperty
    String rtspPath = "/11";

    /**
     * The path on the camera to the snapshot if different from /web/tmpfs/auto.jpg
     */
    @JsonProperty
    String jpegPath = "/web/tmpfs/auto.jpg";


    /**
     *
     */
    @JsonProperty
    String user = "admin";

    @JsonProperty
    String password = "admin";

    // TODO: Use ONVIF to interrogate the camera


}
