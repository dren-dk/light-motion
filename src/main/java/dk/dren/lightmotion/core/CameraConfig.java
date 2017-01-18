package dk.dren.lightmotion.core;

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
    private String address;

    /**
     * The human-readable name/location of the camera, if left empty the address is used
     */
    @NotEmpty
    @JsonProperty
    private String name;

    /**
     * The user name used for accessing the camera
     */
    @JsonProperty
    private String user = "admin";

    /**
     * The password used for accessing the camera, beware that the password is stored in clear text on the camera and
     * it will be transmitted in clear text in several protocols.
     */
    @JsonProperty
    private String password = "admin";

    /**
     * The profile number to stream from
     */
    @JsonProperty
    private Integer profileNumber = 0;

    /**
     * The framerate of the stream, set only if the camera lies in the ONVIF response
     */
    @JsonProperty
    private Integer forceFramerate;

    /**
     * The width of the image in the stream, set only if the camera lies in the ONVIF response
     */
    @JsonProperty
    private Integer forceWidth;

    /**
     * The height of the image in the stream, set only if the camera lies in the ONVIF response
     */
    @JsonProperty
    private Integer forceHeight;
}
