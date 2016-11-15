package dk.dren.dwa;

import io.dropwizard.Configuration;
import lombok.Data;


/**
 * Add fields for the configuration in this class,
 * Dropwizard will take care of reading the server,yaml file.
 * Don't bother creating getters and setters, @Data from Lombok creates getters and setters automagically.
 */
@Data
public class ServerConfiguration extends Configuration {

}