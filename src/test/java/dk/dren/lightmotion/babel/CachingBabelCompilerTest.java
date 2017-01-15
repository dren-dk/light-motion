package dk.dren.lightmotion.babel;

import dk.dren.lightmotion.cache.DiskCache;
import dk.dren.lightmotion.webjars.WebJars;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by ff on 21-11-16.
 */
public class CachingBabelCompilerTest {



    @Test
    public void compileWebJar() throws Exception {
        final String JS6 = readFromClassPath("test.js");

        File cacheDir = File.createTempFile("DiskCacheTest", ".dir");
        cacheDir.delete();
        cacheDir.mkdir();
        try {
            WebJars webJars = new WebJars();
            CachingBabelCompiler compiler = new CachingBabelCompiler(new BabelCompiler(webJars), new DiskCache(cacheDir, 1000 * 1000));

            String testJS = compiler.compileResource("test.jsx");
            assertEquals("The output really ought to match", JS6, testJS);
        } finally {
            FileUtils.forceDelete(cacheDir);
        }
    }

    private static String readFromClassPath(String s) throws IOException {
        try (InputStream is = CachingBabelCompilerTest.class.getClassLoader().getResourceAsStream(s)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void compileResource() throws Exception {

    }

    @Test
    public void compileFile() throws Exception {

    }

}