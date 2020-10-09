package com.nchain.jcl.script.interpreter;

import com.nchain.jcl.script.config.ScriptConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A builder for instances of Script Interpreter.
 */
public class ScriptInterpreterBuilder {

    private final String id;
    private ScriptConfig scriptConfig;

    public ScriptInterpreterBuilder(String id) {
        this.id = id;
    }

    public ScriptInterpreterBuilder config(ScriptConfig scriptConfig) {
        this.scriptConfig = scriptConfig;
        return this;
    }

    public ScriptInterpreter build() {
        return new ScriptInterpreter(id, scriptConfig);
    }
}
