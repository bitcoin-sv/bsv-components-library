package com.nchain.jcl.script.exception;

import com.nchain.jcl.base.exception.VerificationException;
import com.nchain.jcl.script.interpreter.ScriptInterpreter;

/**
 * @author 2020 Steve Shadders.
 * Copyright (c) 2018-2020 nChain Ltd
 */

@SuppressWarnings("serial")
public class ScriptExecutionException extends VerificationException {

    private ScriptInterpreter.ScriptExecutionState state;

    public ScriptExecutionException(ScriptInterpreter.ScriptExecutionState state, String msg) {
        super(msg);
        this.state = state;
    }

    public ScriptExecutionException(String msg) {
        super(msg);
    }

    public ScriptInterpreter.ScriptExecutionState getState() {
        return state;
    }
}
