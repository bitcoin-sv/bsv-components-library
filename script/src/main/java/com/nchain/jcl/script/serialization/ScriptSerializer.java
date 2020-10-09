package com.nchain.jcl.script.serialization;

import com.nchain.jcl.script.core.Script;
import com.nchain.jcl.script.core.ScriptChunk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for the Script. As other Serialzers in JCL, its a stateless Singletong instance.
 */
public class ScriptSerializer {

    private static ScriptSerializer instance;

    private ScriptSerializer() {}

    public static ScriptSerializer getInstance() {
        if (instance == null) {
            synchronized (ScriptSerializer.class) {
                instance = new ScriptSerializer();
            }
        }
        return instance;
    }

    public Script deserialize(ByteArrayInputStream bis) {
        try {
            // Apart from the list of chunks, we also need the raw data, so we make a copy of it first:
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bis.transferTo(bos);
            byte[] program = bos.toByteArray();
            ByteArrayInputStream bisDuplicated = new ByteArrayInputStream(program);

            List<ScriptChunk> chunks = new ArrayList<>();
            int initialSize = bisDuplicated.available();
            while (bisDuplicated.available() > 0) {
                int startLocationInProgram = initialSize - bisDuplicated.available();
                ScriptChunk chunk = ScriptChunkSerializer.getInstance().deserialize(startLocationInProgram, bisDuplicated);

                // Save some memory by eliminating redundant copies of the same chunk objects.
                for (ScriptChunk c : ScriptChunk.STANDARD_TRANSACTION_SCRIPT_CHUNKS) {
                    if (c.equals(chunk)) chunk = c;
                }
                chunks.add(chunk);
            } // while...
            Script result = Script.builder().addChunks(chunks).program(program).build();
            return result;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public byte[] serialize(Script script) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (script.getChunks() != null) {
            for (ScriptChunk chunk : script.getChunks()) {
                ScriptChunkSerializer.getInstance().serialize(chunk, bos);
            }
        }
        return bos.toByteArray();
    }
}
