package com.nchain.jcl.script.wiki;

import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.script.core.Script;
import com.nchain.jcl.script.core.ScriptOpCodes;
import com.nchain.jcl.script.interpreter.ScriptContext;
import com.nchain.jcl.script.interpreter.ScriptInterpreter;
import com.nchain.jcl.script.serialization.ScriptSerializer;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-10-05
 */
public class WikiExamples {

    @Ignore
    public void testSimpleScript() {
        try {

            Script script = Script.builder()
                                .smallNum(2)
                                .smallNum(2)
                                .op(ScriptOpCodes.OP_ADD)
                                .smallNum(4)
                                .op(ScriptOpCodes.OP_EQUAL)
                                .build();

            byte[] scriptContent = ScriptSerializer.getInstance().serialize(script);
            System.out.println("Script Serialized: " + HEX.encode(scriptContent));

            Script script2 = Script.builder(scriptContent).build();

            ScriptInterpreter interpreter = ScriptInterpreter.builder("demo").build();
            ScriptContext scriptContext = ScriptContext.builder().build();
            interpreter.execute(scriptContext, script);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
