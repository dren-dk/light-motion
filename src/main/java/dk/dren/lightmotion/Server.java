package dk.dren.lightmotion;

import dk.dren.lightmotion.config.ServerConfiguration;
import dk.dren.lightmotion.core.LightMotion;
import dk.dren.lightmotion.db.Database;
import dk.dren.lightmotion.healthchecks.DiskSpaceCheck;
import dk.dren.lightmotion.injectors.InjectorBinder;
import dk.dren.lightmotion.resources.FrontPageResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.skife.jdbi.v2.DBI;

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
		//bootstrap.addCommand(new SomeCommand());
		bootstrap.addBundle(new MigrationsBundle<ServerConfiguration>() {
			@Override
			public PooledDataSourceFactory getDataSourceFactory(ServerConfiguration serverConfiguration) {
				return serverConfiguration.getLightMotion().getDatabase();
			}
		});

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

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(environment, configuration.getLightMotion().getDatabase(), "postgresql");
        Database database = jdbi.onDemand(Database.class);

        // Register injectors.
		LightMotion cameraManager = new LightMotion(database, configuration.getLightMotion());

		environment.jersey().register(new InjectorBinder(configuration, jdbi, database));

		// Register healthchecks, there really should be many more than just one.
		environment.healthChecks().register("Disk-space", new DiskSpaceCheck());

		// Register resources
		environment.jersey().register(FrontPageResource.class);

		environment.lifecycle().manage(cameraManager);
	}

}
