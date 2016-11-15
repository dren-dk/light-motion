package dk.dren.dwa.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/babel/{jsx: .*}")
public class BabelResource {
    @GET
    @Produces("text/javascript")
    public Response compile(@PathParam("jsx")String jsx) {
        return Response.ok().build();
    }
}
