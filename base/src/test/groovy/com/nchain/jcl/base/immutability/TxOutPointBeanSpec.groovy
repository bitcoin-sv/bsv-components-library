package com.nchain.jcl.base.immutability

import com.nchain.jcl.base.domain.api.base.TxOutPoint
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class TxOutPointBeanSpec extends Specification {

    def "creating instance and change initial values"() {
        given:
            Integer index = 1
            byte[] hashBytes = new byte[32]
            Sha256Wrapper hash = Sha256Wrapper.of(hashBytes)
            TxOutPoint bean = TxOutPoint.builder()
                        .index(index)
                        .hash(hash)
                        .build()


        when:
            index = 2
        then:
            !bean.getIndex().equals(index)
    }
}
