package com.nchain.jcl.script.immutability

import com.nchain.jcl.script.core.ScriptData
import spock.lang.Specification

class ScriptDataSpec extends Specification {


    def "Testing immutability - Create instance and change original value"() {
        given:
            byte[] bytes = new byte[2]
            bytes[0] = 1
            bytes[1] = 1
            ScriptData data = ScriptData.builder().data(bytes).build()
        when:
            byte[] bytesToChange = data.data()
            bytes[0] = 2
            bytes[1] = 2
        then:
            bytesToChange[0] == 1
            bytesToChange[1] == 1
    }

    def "Testing immutability - Create instance and change"() {
        given:
            byte[] bytes = new byte[2]
            bytes[0] = 1
            bytes[1] = 1
            ScriptData data = ScriptData.builder().data(bytes).build()
        when:
            byte[] bytesToChange = data.data()
            bytesToChange[0] = 2
            bytesToChange[1] = 2
        then:
            bytes[0] == 1
            bytes[1] == 1
            bytesToChange[0] == 2
            bytesToChange[1] == 2
    }

    def "Testing immutability - Copy instance and change"() {
        given:
            byte[] bytes = new byte[2]
            bytes[0] = 1
            bytes[1] = 1
            ScriptData data = ScriptData.builder().data(bytes).build()
            ScriptData dataToChange = data.toBuilder().build()
        when:
            byte[] bytesToChange = dataToChange.data()
            bytesToChange[0] = 2
            bytesToChange[1] = 2
        then:
            bytes[0] == 1
            bytes[1] == 1
            bytesToChange[0] == 2
            bytesToChange[1] == 2
    }
}
