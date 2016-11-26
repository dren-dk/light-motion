package dk.dren.dwa.babel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.script.Bindings;
import javax.script.ScriptEngine;

@RequiredArgsConstructor
@Getter
public class ScriptEngineAndBindings {
    private final ScriptEngine engine;
    private final Bindings bindings;
}
