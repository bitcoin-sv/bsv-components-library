package com.nchain.jcl.script.exception;


/**
 * Steve Shadders
 * Copyright (c) 2018-2020 nChain Ltd
 */

import com.nchain.jcl.base.exception.VerificationException;

public class ScriptParseException extends VerificationException {

    public ScriptParseException(String msg) {
        super(msg);
    }

    public ScriptParseException(Exception e) {
        super(e);
    }

    public ScriptParseException(String msg, Throwable t) {
        super(msg, t);
    }
}
