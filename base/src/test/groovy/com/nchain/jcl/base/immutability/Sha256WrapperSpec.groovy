package com.nchain.jcl.base.immutability

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class Sha256WrapperSpec extends Specification {

    def "creating instance and change original values"() {
        given:
            byte[] hashBytes = new byte[32]
            Sha256Wrapper hash = Sha256Wrapper.of(hashBytes)
        when:
            hashBytes[1] = 5
            hashBytes[2] = 5
        then:
            hash.getBytes()[1] != 5 || hash.getBytes()[2] != 5
    }

    def "getting bytes form instnace and change them"() {
       given:
            byte[] hashBytes = new byte[32]
            Sha256Wrapper hash = Sha256Wrapper.of(hashBytes)
       when:
            hash.getBytes()[1] = 5
            hash.getBytes()[2] = 5
       then:
            hash.getBytes()[1] != 5 || hash.getBytes()[2] != 5
    }

    def "getting reversed bytes form instnace and change them"() {
        given:
            byte[] hashBytes = new byte[32]
            Sha256Wrapper hash = Sha256Wrapper.of(hashBytes)
        when:
            hash.getReversedBytes()[1] = 5
            hash.getReversedBytes()[2] = 5
        then:
            hash.getReversedBytes()[1] != 5 || hash.getReversedBytes()[2] != 5
    }
}
