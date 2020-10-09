package com.nchain.jcl.script.interpreter;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.script.core.Script;
import lombok.Builder;
import lombok.Value;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A ScriptContext stores information that is "external" from the Script, meaning that is information that is NOT
 * contained in the Script itself nor it can be found in the Stack during the Script execution. This information
 * needs to be provided to the Script Engine in order to run it properly.
 */

@Value
public class ScriptContext {

    /**
     * The Stack used to run the Script. If not provided, and empty one is used. But another pre-filled one can also
     * be used, for example one form a previous execution of a previous Script.
     */
    private ScriptStack stack;

    /** The Trnsaction containing this Script */
    private Tx txContainingScript;

    /**
     * The Index of the OUTPUT of this transaction (txContainingScript) that we are trying to SPEND.
     * This applies to the scenario where we are checking both the "locking" and "unlocking" script.
     */
    private long txInputIndex;

    /** Pending... */
    private Coin value;

    /**
     * A Listener we can inject to trigger Events on the Client side andbe notified about the Script execution. Util
     * for debugging.
     */
    private ScriptStateListener listener;

    @Builder(toBuilder = true)
    public ScriptContext(ScriptStack stack,
                         Tx txContainingScript,
                         long txInputIndex,
                         Coin value,
                         ScriptStateListener listener) {

        // Defensive copy, to enforce immutability:
        this.stack = (stack == null)? new ScriptStack() : new ScriptStack(stack);

        this.txContainingScript = txContainingScript;
        this.txInputIndex = txInputIndex;

        this.value = (value == null)? Coin.ZERO : value;

        // TODO: NOTE: Any possible to way to PROTECT the listener from being called from somewhere else than the Script Interpreter???
        this.listener = listener;
    }

    // Overwriting the default getter, to enforce immutability
    public ScriptStack getStack() {
        return (stack == null)? null : new ScriptStack(stack);
    }

}
