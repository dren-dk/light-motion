package dk.dren.lightmotion.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.File;
import java.util.List;

/**
 * All of the light motion specific configuration.
 */
@Data
public class LightMotionConfig {

    /**
     * The cameras to pull from
     */
    @NotEmpty
    @JsonProperty
    private List<CameraConfig> cameras;

    /**
     * The minimum interval between polling of jpeg snapshots from each camera in milliseconds.
     */
    @JsonProperty
    private Integer pollInterval = 2000;

    /**
     * The directory to store temporary files in, each camera will record into this directory, so there will be a lot
     * of sequential writing to a number of files in parallel, so it might eat an SSD too quickly.
     *
     * Default is to use ${HOME}/.light-motion/working
     */
    @JsonProperty
    private File workingRoot = new File(System.getProperty("user.home"), ".light-motion/working");

    /**
     * The directory where the recordings are written when motion is detected.
     *
     * Default is to use ${HOME}/.light-motion/recordings
     */
    @JsonProperty
    private File recordingRoot = new File(System.getProperty("user.home"), ".light-motion/recording");


    /**
     * The number of seconds to record in each chunk before the movement happens, default is 60 seconds.
     */
    @JsonProperty
    private final Integer chunkLength = 60;
}


