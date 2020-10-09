package com.nchain.jcl.script.interpreter;

import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores the result of the Script Execution.
 */

@Value
public class ScriptResult {
    private final ScriptStack stack;

    @Builder
    public ScriptResult(ScriptStack stack) {
        // defensive copy, to enforce immutability
        this.stack = (stack == null)? null : new ScriptStack(stack);
    }

    // Overwritting default getter, to enfornce immutability
    public ScriptStack getStack() {
        return (stack == null)? null : new ScriptStack(stack);
    }

    /** Indicates if the Result of the Script execution is OK */
    public boolean isOk() {
        return (stack != null && ScriptUtils.castToBool(stack.pollLast().bytes()));
    }
}
