package com.nchain.jcl.script.immutability

import com.nchain.jcl.script.core.ScriptChunk
import com.nchain.jcl.script.core.ScriptData
import spock.lang.Specification

class ScriptChunkSpec extends Specification {

    def "testing immutability - Create instance and change original data"() {
        given:
            byte[] bytes = new byte[2]
            bytes[0] = 1
            bytes[1] = 1
            ScriptData data = ScriptData.builder().data(bytes).build()
            Integer opCode = 1
            ScriptChunk chunk = ScriptChunk.builder().data(data).opcode(opCode).build()
        when:
            opCode = 2;
        then:
            chunk.getOpcode() == 1
    }

    def "testing immutability - Copying instance and change original data"() {
        given:
            byte[] bytes = new byte[2]
            bytes[0] = 1
            bytes[1] = 1
            ScriptData data = ScriptData.builder().data(bytes).build()
            Integer opCode = 1
            ScriptChunk chunk = ScriptChunk.builder().data(data).opcode(opCode).build()
            ScriptChunk chunkToChange = chunk.toBuilder().build()
        when:
            opCode = 2;
        then:
            chunk.getOpcode() == 1
            chunkToChange.getOpcode() == 1
    }

}
