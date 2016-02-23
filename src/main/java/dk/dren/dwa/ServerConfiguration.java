package dk.dren.dwa;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import lombok.Data;

@Data
public class ServerConfiguration extends Configuration {
	// Add fields for the configuration here, don't bother creating getters and setters
	// @Data from Lombok creates getters and setters automagically.
	
	
	@JsonProperty("swagger")
    public SwaggerBundleConfiguration swaggerBundleConfiguration;
}