package dk.dren.lightmotion.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.db.DataSourceFactory;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.List;

/**
 * All of the light motion specific configuration.
 */
@Data
public class LightMotionConfig {

    /**
     * The cameras to pull data from, these are inserted into the database, unless they already exist.
     * If an existing camera exists with the same name it's updated with the data found in the yaml
     */
    @NotEmpty
    @JsonProperty
    private List<CameraConfig> cameras;

    /**
     * The minimum interval between polling of jpeg snapshots from each camera in milliseconds, this is not used if
     * lowres streaming is used.
     */
    @JsonProperty
    private Integer pollInterval = 2000;

    /**
     * The directory to store temporary files in, each camera will record into this directory, so there will be a lot
     * of sequential writing to a number of files in parallel, so it might eat an SSD too quickly.
     *
     * Default is to use /run/user/${uid}/light-motion if possible or /tmp/light-motion otherwise
     */
    @JsonProperty
    private File workingRoot = TmpFsFinder.getDefaultTmpFs();

    /**
     * The directory where the long-term state is stored.
     *
     * Default is to use ${HOME}/.light-motion/state
     */
    @JsonProperty
    private File stateRoot = new File(System.getProperty("user.home"), ".light-motion/state");

    /**
     * The directory where the recordings are written before motion is detected.
     *
     * 60 MB per minute per camera is written here.
     *
     * Default is to use ${HOME}/.light-motion/pre-record
     */
    @JsonProperty
    private File chunkRoot = new File(System.getProperty("user.home"), ".light-motion/chunks");

    /**
     * The number of seconds to record in each chunk, default is 10 seconds.
     *
     * The minimum recording size will be chunkLength*(chunksBeforeDetection+chunksAfterDetection) seconds long
     */
    @JsonProperty
    private final Integer chunkLength = 10;

    /**
     * The number of chunks to keep before movement was detected
     *
     * The minimum recording size will be chunkLength*(chunksBeforeDetection+chunksAfterDetection) seconds long
     */
    @JsonProperty
    private final Integer chunksBeforeDetection = 2;

    /**
     * The number of chunks to keep after movement was no-longer detected
     *
     * The minimum recording size will be chunkLength*(chunksBeforeDetection+chunksAfterDetection) seconds long
     */
    @JsonProperty
    private final Integer chunksAfterDetection = 2;

    /**
     * Number of chunks to keep, regardless of detection.
     *
     * With the default chunk length of 10 seconds, 24 hours worth of chunks are kept.
     */
    @JsonProperty
    private final Integer chunkRetention = 6*60*24;


    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();
}
