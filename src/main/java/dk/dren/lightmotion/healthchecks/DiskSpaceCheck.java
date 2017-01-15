package dk.dren.lightmotion.healthchecks;

import com.codahale.metrics.health.HealthCheck;

import java.io.File;

/**
 * This is just an example of a healthcheck, I doubt it's actually useful.
 */
public class DiskSpaceCheck extends HealthCheck {
    @Override
    protected Result check() throws Exception {
        File self = new File(DiskSpaceCheck.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        long gigs = self.getFreeSpace()/(1024*1024*1024);
        if (gigs < 10) {
            return Result.unhealthy("There's only "+ gigs +" GB free space");
        } else {
            return Result.healthy("There's "+ gigs +" GB free space, so that's nice");
        }
    }
}
