package dk.dren.lightmotion.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @JsonProperty
    private Boolean storeSnapshots = false;
}
