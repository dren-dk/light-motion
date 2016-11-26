package dk.dren.dwa.babel;

import dk.dren.dwa.webjars.WebJars;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by ff on 20-11-16.
 */
public class BabelCompilerTest {
    @Test
    public void compile() throws Exception {
        BabelCompiler bc = new BabelCompiler(new WebJars());

        for (int i=0;i<5;i++) {
            long t0 = System.currentTimeMillis();
            String output = bc.compile("<Component />");
            long duration = System.currentTimeMillis()-t0;

            System.out.println("Time taken: "+duration+" ms to produce: "+output); // First one: 20 sec, the rest about 41 ms.
            assertEquals("React.createElement(Component, null);", output);

            if (i > 0) {
                assertTrue("Duration should be lower in iteration " + i+" was "+duration+" ms", duration < 200);
            }
        }
    }

}