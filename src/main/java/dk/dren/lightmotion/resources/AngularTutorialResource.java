package dk.dren.dwa.resources;

import dk.dren.dwa.api.Phone;
import dk.dren.dwa.db.PhoneDB;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.List;

/**
 * This resource is where the the REST calls for STEP 5 in the angular tutorial lives
 * in the tutorial a static json file is used, but producing json is what DW is good at
 * so I chose to bring the server into play at this point.
 */
@Api(value = "Phone Cat", description = "Angular Tutorial phone cat services")
@Path("/phonecat")
public class AngularTutorialResource {

    @Inject
    private PhoneDB phoneDB;

    @ApiOperation(value = "Lists all phones", response = Phone.class, responseContainer="List")
    @Path("phones")
    @GET
    @Produces("application/json")
    public List<Phone> getPhones() throws IOException {
        return phoneDB.findAllPhones();
    }
}
