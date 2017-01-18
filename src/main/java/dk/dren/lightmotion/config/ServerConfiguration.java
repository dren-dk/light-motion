package dk.dren.lightmotion.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dren.lightmotion.core.LightMotionConfig;
import io.dropwizard.Configuration;
import lombok.Data;

import javax.validation.constraints.NotNull;


/**
 * This is the main configuration class, note that all the LM specific stuff is in a separate class, this is to make
 * it possible to use the camera and motion detection code from other applications in the future.
 */
@Data
public class ServerConfiguration extends Configuration {

    @NotNull
    @JsonProperty
    private LightMotionConfig lightMotion;
}