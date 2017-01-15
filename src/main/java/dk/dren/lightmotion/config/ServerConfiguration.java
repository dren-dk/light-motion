package dk.dren.lightmotion.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;


/**
 * Add fields for the configuration in this class,
 * Dropwizard will take care of reading the server,yaml file.
 * Don't bother creating getters and setters, @Data from Lombok creates getters and setters automagically.
 */
@Data
public class ServerConfiguration extends Configuration {

    @NotEmpty
    @JsonProperty
    private List<CameraConfig> cameras;

}