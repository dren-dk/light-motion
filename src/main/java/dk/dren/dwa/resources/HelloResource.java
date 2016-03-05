package dk.dren.dwa.resources;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import dk.dren.dwa.ServerConfiguration;
import dk.dren.dwa.injectors.UserAgent;

/**
 * This is an example REST service that takes a simple path parameter.
 */
@Api(value = "/hello", description = "Various kinds of saying hello")
@Path("/hello/")
public class HelloResource {
	// An instance of the thing
	@Inject
	UserAgent userAgent;
	
	// Our singleton
	@Inject
	ServerConfiguration config;

	
	@ApiOperation(value = "Says hello with plain text")
	@Path("text/{world}")
	@GET
	@Timed
	@Produces("text/plain")
	public String get(@ApiParam(value = "The thing to greet, possibly the world", required = true) @PathParam("world")String world) {
		return "Hello "+world; 
	}
}
