package com.nchain.jcl.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.core.Script;
import com.nchain.jcl.script.core.ScriptVerifyFlag;


import com.nchain.jcl.script.exception.ScriptExecutionException;
import com.nchain.jcl.script.interpreter.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStreamReader;
import java.util.*;


public class InterpreterDataTest {

    private static final Logger log = LoggerFactory.getLogger(InterpreterDataTest.class);


    @Test
    public void testValidScriptsFromData() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream("/script_valid.json"), Charsets.UTF_8));
        //JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream("/testingJCL.json"), Charsets.UTF_8));

        ScriptInterpreter interpreter = ScriptInterpreter.builder("validTest-Interpreter").build();
        for (JsonNode test : json) {
            try {
                parseDataAndTest(interpreter, test);
            } catch (Exception exception) {
                System.err.println(test);

                throw exception;
            }
        }
    }

    @Test
    public void testInvalidScriptsFromData() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream("/script_invalid.json"), Charsets.UTF_8));
        //JsonNode json = new ObjectMapper().readTree(new InputStreamReader(getClass().getResourceAsStream("/testingJCL.json"), Charsets.UTF_8));

        ScriptInterpreter interpreter = ScriptInterpreter.builder("invalidTest-Interpreter").build();
        for (JsonNode test : json) {
            try {
                parseDataAndTest(interpreter, test);
            } catch (Exception ex) {
                log.debug("Expected test failure failed with reason: " + ex.getMessage());
                continue;
            }

            throw new Exception("Test: '" + test + "' passed unexpectedly.");
        }
    }


    private void printStack(List<StackItem> stack) {
        if (stack != null) {
            System.out.println(" > Stack:");
            for (int i = stack.size() - 1; i >=0; i--) {
                System.out.println("    - " + stack.get(i).toString());
            }
        }
    }

    private void parseDataAndTest(ScriptInterpreter interpreter, JsonNode test) throws Exception {

        // We build all the structures needed to run the Script:
        Script scriptSig = Script.builder(test.get(0).asText()).build();
        Script scriptPubKey = Script.builder(test.get(1).asText()).build();
        Set<ScriptVerifyFlag> verifyFlags = parseVerifyFlags(test.get(2).asText());
        Tx txSpend = buildSpendingTransaction(scriptSig);
        ScriptConfig scriptConfig = ScriptConfig.builder(verifyFlags).build();


        System.out.println(test.get(0).asText() + " " + test.get(1).asText());

        // WE define a simple listener, por logging purposes
        ScriptStateListener listener = new ScriptStateListener() {
            @Override
            public void onBeforeOpCodeExecuted(boolean willExecute) {}
            @Override
            public void onAfterOpCodeExectuted() {
                // We print the Stack:
                //System.out.println("> Current Chunk: " + this.getCurrentChunk());
                //printStack(this.getStack());
            }
            @Override
            public void onExceptionThrown(ScriptExecutionException exception) {}
            @Override
            public void onScriptComplete() {}
        };

        ScriptContext scriptContext = ScriptContext.builder()
                .txContainingScript(txSpend)
                .txInputIndex(0) // Always ZERO, for testing purposes...
                .listener(listener)
                .build();

        //interpreter.correctlySpends(scriptConfig, scriptSig, scriptPubKey, txSpend, 0, Coin.ZERO, listener);
        interpreter.execute(scriptConfig, scriptContext, scriptSig, scriptPubKey);
    }


    public Tx buildSpendingTransaction(Script signature) {
        //return new TxBean(null, 1, 0, inputs, Collections.emptyList());
        TxOutPoint creditTxOutpoint = TxOutPoint.builder().hash(Sha256Wrapper.ZERO_HASH).build();
        TxInput txInput = TxInput.builder().outpoint(creditTxOutpoint).scriptBytes(signature.getProgram()).build();
        Tx tx = Tx.builder().inputs(Arrays.asList(txInput)).outputs(Collections.emptyList()).build();
        return tx;

    }



    private Set<ScriptVerifyFlag> parseVerifyFlags(String str) {
        Set<ScriptVerifyFlag> flags = EnumSet.noneOf(ScriptVerifyFlag.class);
        if (!"NONE".equals(str)) {
            for (String flag : str.split(",")) {
                try {
                    flags.add(ScriptVerifyFlag.valueOf(flag));
                } catch (IllegalArgumentException x) {
                    log.debug("Cannot handle verify flag {} -- ignored.", flag);
                }
            }
        }
        return flags;
    }

}
