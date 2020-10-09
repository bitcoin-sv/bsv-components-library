package com.nchain.jcl.script.immutability

import com.nchain.jcl.script.interpreter.ScriptContext
import com.nchain.jcl.script.interpreter.ScriptStack
import com.nchain.jcl.script.interpreter.StackItem
import spock.lang.Specification

class ScriptContextSpec extends Specification {
    def "creating instance and change original values"() {
        given:
            ScriptStack stack = new ScriptStack()
            ScriptContext context = ScriptContext.builder()
                .stack(stack)
                .build()
        when:
            stack.add(StackItem.forSmallNum(5))
        then:
            context.getStack().size() == 0
    }
}
