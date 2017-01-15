package dk.dren.lightmotion;

import dk.dren.lightmotion.config.ServerConfiguration;
import dk.dren.lightmotion.db.PhoneDB;
import dk.dren.lightmotion.healthchecks.DiskSpaceCheck;
import dk.dren.lightmotion.injectors.InjectorBinder;
import dk.dren.lightmotion.resources.AngularTutorialResource;
import dk.dren.lightmotion.resources.BabelResource;
import dk.dren.lightmotion.resources.FrontPageResource;
import dk.dren.lightmotion.resources.HelloResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

/**
 * This is the main bootstrapping class of the application, it's what brings all the bits together and starts the application
 */
public class Server extends Application<ServerConfiguration>{

	/**
	 * This is the main entry point for the application, regardless of how it's started.
	 */
	public static void main(String[] args) throws Exception {
        try {
        	new Server().run(args);
        } catch (Throwable t) {
        	System.err.println("Failed while starting the application, giving up");
        	t.printStackTrace(System.err);
        	System.exit(254);
        }
    }

	/**
	 * Just set a human readable name of the application
	 */
	@Override
	public String getName() {
		return "Light Motion";
	}


	/**
	 * Early startup bootstrapping, this is where all the bundles and the commands that might be needed
	 * are added, this is called before the command line parser figures out what we're actually starting.
	 */
	@Override
	public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
		// Add commands		
		//bootstrap.addCommand(new SomeCommand());
		
		// We'll need to serve static files from our own classpath, this causes /static/ in the classpath to be exposed as /static/
		bootstrap.addBundle(new AssetsBundle("/META-INF/resources/webjars/", "/webjars/", "index.html", "webjars"));
		bootstrap.addBundle(new AssetsBundle("/static/", "/static/", "index.html", "static"));

		SwaggerBundleConfiguration swaggerConfig = new SwaggerBundleConfiguration();
		swaggerConfig.setResourcePackage("dk.dren.lightmotion.resources");
		bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>() {
	        @Override
	        protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(ServerConfiguration configuration) {
	            return swaggerConfig;
	        }
	    });

		bootstrap.addBundle(new MultiPartBundle());
	}

	@Override
	public void run(ServerConfiguration configuration, Environment environment) throws Exception {
		// Register injectors.
		environment.jersey().register(new InjectorBinder(configuration, new PhoneDB()));

		// Register healthchecks, there really should be many more than just one.
		environment.healthChecks().register("Disk-space", new DiskSpaceCheck());

		// Register resources
		environment.jersey().register(HelloResource.class);
		environment.jersey().register(FrontPageResource.class);
		environment.jersey().register(AngularTutorialResource.class);
		environment.jersey().register(BabelResource.class);
	}

}
