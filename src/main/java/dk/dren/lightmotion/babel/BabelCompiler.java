package dk.dren.dwa.babel;

import dk.dren.dwa.webjars.WebJars;
import lombok.extern.java.Log;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 * A thread safe, caching babel compiler.
 *
 * As compiling Babel itself is quite heavy (20 seconds on my laptop) the script engines that have babel loaded
 * are kept in a pool, this arrangement also takes care of ensuring that only one thread accesses an engine at once.
 */
@Log
public class BabelCompiler {
    private final WebJars webJars;
    private final GenericObjectPool<ScriptEngineAndBindings> enginePool;

    public BabelCompiler(WebJars webJars) {
        this.webJars = webJars;
        enginePool = new GenericObjectPool<>(new BasePooledObjectFactory<ScriptEngineAndBindings>() {
            @Override
            public ScriptEngineAndBindings create() throws Exception {
                return createEngine();
            }

            @Override
            public PooledObject<ScriptEngineAndBindings> wrap(ScriptEngineAndBindings obj) {
                return new DefaultPooledObject<ScriptEngineAndBindings>(obj);
            }
        });
    }

    private ScriptEngineAndBindings createEngine() throws IOException, ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("text/javascript");
        SimpleBindings bindings = new SimpleBindings();
        engine.eval(webJars.open("babel-standalone", "babel.min.js").getContent(), bindings);
        return new ScriptEngineAndBindings(engine, bindings);
    }

    public String compile(String jsx) throws ScriptException {
        ScriptEngineAndBindings engine = null;
        try {
            engine = enginePool.borrowObject();
            engine.getBindings().put("input", jsx);
            return (String)engine.getEngine().eval("Babel.transform(input, { presets: ['react'] }).code", engine.getBindings());
        } catch (ScriptException e) {
            log.log(Level.SEVERE, "Failed to compile jsx\n" + jsx, e);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (engine != null) {
                enginePool.returnObject(engine);
            }
        }
    }
}
