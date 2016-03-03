package dk.dren.dwa.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;

/**
 * Serves up the front page.
 */
@Path("/")
public class FrontPageResource {
    @GET
    @Timed
    @Produces("text/html")
    public StreamingOutput get() {
        return outputStream -> {
            try (InputStream is = FrontPageResource.class.getResourceAsStream("/static/index.html")) {
                IOUtils.copy(is, outputStream);
            }
        };
    }
}