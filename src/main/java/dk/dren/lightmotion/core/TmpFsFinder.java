package dk.dren.lightmotion.core;

import com.sun.jna.platform.unix.LibC;
import lombok.extern.java.Log;

import java.io.File;

/**
 * Finds a file system location where tmpfs could be mounted.
 */
@Log
public class TmpFsFinder {
    public static File getDefaultTmpFs() {
        int uid = LibC.INSTANCE.geteuid();

        File uidTmpFs = new File("/run/user/"+uid);
        if (uidTmpFs.isDirectory() && uidTmpFs.canWrite()) {
            return new File(uidTmpFs, "light-motion");
        }

        log.warning("Cannot write to "+uidTmpFs+" falling back to /tmp, please set tmp to a tmpfs");
        return new File("/tmp/light-motion");
    }
}
