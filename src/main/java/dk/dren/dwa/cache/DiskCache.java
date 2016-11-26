package dk.dren.dwa.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * This is a cache that stores all entries on disk forever and the latest n elements in memory,
 */
public class DiskCache {
    private final File cacheDir;
    private final Cache<String, String> memCache;
    private final PrintStream diskLog;

    public DiskCache(File cacheDir, int ramSize) throws FileNotFoundException {
        this.cacheDir = cacheDir;
        memCache = CacheBuilder.newBuilder()
                .maximumWeight(ramSize)
                .weigher((Weigher<String, String>) (key, value) -> value.length())
                .build();
        diskLog = new PrintStream(new FileOutputStream(new File(cacheDir, "cache.log"), true));
    }

    public String get(String key, String description, Callable<String> computer) throws Exception {
        String result = memCache.getIfPresent(key);
        if (result != null) {
            return result;
        }

        File cacheFile = new File(cacheDir, key);
        long t0 = System.currentTimeMillis();
        if (cacheFile.isFile()) {
            result = FileUtils.readFileToString(cacheFile, "UTF-8");
            diskLog.println(t0+"\tuse\t"+key);

        } else {
            result = computer.call();
            long duration = System.currentTimeMillis()-t0;

            synchronized (this) {
                File tmpFile = new File(cacheDir, key + ".tmp");
                FileUtils.write(tmpFile, result, "UTF-8");
                FileUtils.moveFile(tmpFile, cacheFile);
            }
            diskLog.println(t0+"\tadd\t"+key+"\t"+duration+"\t"+description);
        }

        memCache.put(key, result);

        return result;
    }


}
