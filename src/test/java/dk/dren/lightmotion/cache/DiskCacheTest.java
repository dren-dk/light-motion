package dk.dren.lightmotion.cache;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by ff on 21-11-16.
 */
public class DiskCacheTest {

    @Test
    public void testDisk() throws Exception {
        File cacheDir = File.createTempFile("DiskCacheTest", ".dir");
        cacheDir.delete();
        cacheDir.mkdir();
        try {
            DiskCache cache = new DiskCache(cacheDir, 1000);

            final String input = "Test content"+System.currentTimeMillis()+" ";
            final String correctOutput = "output:"+input;

            String result = cache.get("test", "test entry", ()-> "output:"+input);

            assertEquals("The content of the output must match", correctOutput, result);

            String result2 = cache.get("test", "test entry", ()-> "Cache miss");

            assertEquals("The content of the output must match because it must come from the cache", correctOutput, result2);

            DiskCache cache2 = new DiskCache(cacheDir, 1000);
            String result3 = cache2.get("test", "test entry", ()-> "Cache miss");

            assertEquals("The content of the output must match because it must come from the cache", correctOutput, result3);


            for (int i=0; i<1000; i++) {
                final int fi = i;
                String output = cache.get("test:"+i, "test entry", ()-> "output:"+input+":"+fi);
                assertEquals("The content of the output must match", correctOutput+":"+i, output);
            }

            for (int i=0; i<1000; i++) {
                final int fi = i;
                String output = cache.get("test:"+i, "test entry", ()-> "Cache miss");
                assertEquals("The content of the output must match", correctOutput+":"+i, output);
            }


        } finally {
            FileUtils.forceDelete(cacheDir);
        }
    }

}