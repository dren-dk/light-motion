package dk.dren.dwa.injectors;

import java.util.logging.Level;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.glassfish.hk2.api.Factory;

import lombok.extern.java.Log;

/**
 * This takes care of creating and disposing of Thing instances, this is handy when you want to create something per-request,
 * rather than keep a singleton around.
 */
@Log
public class UserAgentFactory implements Factory<UserAgent> {

	@Inject
    private HttpServletRequest request;
	
	@Override
	public UserAgent provide() {
		return new UserAgent(request.getHeader("User-Agent"));
	}

	@Override
	public void dispose(UserAgent instance) {
		try {
			instance.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to dispose of instance", e);
		}
	}

}
