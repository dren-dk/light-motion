package dk.dren.lightmotion.webjars;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Try out the WebJars class
 */
public class WebJarsTest {
    @Test
    public void open() throws Exception {
        WebJars wj = new WebJars();

        WebJarEntry entry = wj.open("babel-standalone", "babel.js");
        IOUtils.copy(entry.getInputStream(), System.err);
    }

}