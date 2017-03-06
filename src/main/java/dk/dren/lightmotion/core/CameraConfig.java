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
     * The profile number to stream high resolution video from
     */
    @JsonProperty
    private Integer profileNumber = 0;

    /**
     * The profile number to stream low res / low fps snapshots from
     */
    @JsonProperty
    private Integer lowresProfileNumber = 1;

    /**
     * True if snapshots should be fetched in stead of streaming the lowres
     */
    @JsonProperty
    private boolean lowresSnapshot = false;

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

    @JsonProperty
    private Boolean storeSnapshots = false;

    /**
     * The port number to use for streaming RTSP from this camera, it must be even
     * The ports from port to port+3 are used, so if you allocate manually space them 4 apart
     *
     * Default is 0 which causes lightmotion to allocate ports automatically
     */
    @JsonProperty
    private Integer rtspClientPort = 0;
}
