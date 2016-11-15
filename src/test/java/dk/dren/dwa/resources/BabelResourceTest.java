package dk.dren.dwa.resources;

import dk.dren.dwa.webjars.WebJarEntry;
import dk.dren.dwa.webjars.WebJars;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;

public class BabelResourceTest {

    @Test
    public void test() throws ScriptException, IOException {
        long bt0 = System.currentTimeMillis();
        WebJars webJars = new WebJars();
        ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("text/javascript");

        SimpleBindings bindings = new SimpleBindings();
        WebJarEntry entry = webJars.open("babel-standalone", "babel.min.js");
        engine.eval(entry.getReader(), bindings);
        long bduration = System.currentTimeMillis()-bt0;

        System.out.println("Bootstrap time: "+bduration+" ms");

        for (int i=0;i<5;i++) {
            long t0 = System.currentTimeMillis();
            bindings.put("input", "<Component />");
            Object output = engine.eval("Babel.transform(input, { presets: ['react'] }).code", bindings);

            long duration = System.currentTimeMillis()-t0;

            System.out.println("Time taken: "+duration+" ms to produce: "+output);

        }
    }
}