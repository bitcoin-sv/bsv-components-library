package com.nchain.jcl.script.immutability

import com.nchain.jcl.script.core.Script
import com.nchain.jcl.script.core.ScriptChunk
import spock.lang.Specification

class ScriptSpec extends Specification {
    def "create Script and change original Values"() {
        given:
            List<ScriptChunk> chunks = new ArrayList<>()
            chunks.add(ScriptChunk.builder().opcode(1).build())
            chunks.add(ScriptChunk.builder().opcode(2).build())
            chunks.add(ScriptChunk.builder().opcode(3).build())

            Script script = Script.builder().addChunks(chunks).build()

        when:
            chunks.remove(1)
            script.getChunks().size() == 3
        then:
            chunks.size() == 2
    }

    def "copy Script and change original Values"() {
        given:
            List<ScriptChunk> chunks = new ArrayList<>()
            chunks.add(ScriptChunk.builder().opcode(1).build())
            chunks.add(ScriptChunk.builder().opcode(2).build())
            chunks.add(ScriptChunk.builder().opcode(3).build())

            Script script = Script.builder().addChunks(chunks).build()
            Script scriptTochange = script.toBuilder().build()
        when:
            chunks.remove(1)
        then:
            chunks.size() == 2
            script.getChunks().size() == 3
            scriptTochange.getChunks().size() == 3
    }
}
