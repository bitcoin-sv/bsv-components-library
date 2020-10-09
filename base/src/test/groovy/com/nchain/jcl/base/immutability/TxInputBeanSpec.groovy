package com.nchain.jcl.base.immutability

import com.nchain.jcl.base.core.Coin
import com.nchain.jcl.base.domain.api.base.TxInput
import com.nchain.jcl.base.domain.bean.base.TxInputBean
import spock.lang.Specification

class TxInputBeanSpec extends Specification {

    def "creating instance and change original values"() {
        given:
            Coin value = Coin.FIFTY_COINS
            byte[] scriptBytes = new byte[10]
            Long sequenceNumber = 5;
            TxInput bean = TxInput.builder()
                    .scriptBytes(scriptBytes)
                    .value(value)
                    .sequenceNumber(sequenceNumber)
                    .build()
        when:
            value = value.add(Coin.FIFTY_COINS)
            sequenceNumber = 6
            scriptBytes[1] = 5
        then:
            !bean.getValue().equals(value)
            bean.getScriptBytes()[1] != 5
            bean.getSequenceNumber() != sequenceNumber
    }

    def "getting values and change them"() {
        given:
            Coin value = Coin.FIFTY_COINS
            byte[] scriptBytes = new byte[10]
            Long sequenceNumber = 5;
            TxInput bean = TxInput.builder()
                .scriptBytes(scriptBytes)
                .value(value)
                .sequenceNumber(sequenceNumber)
                .build()
        when:
            bean.getScriptBytes()[1] = 5
        then:
            bean.getScriptBytes()[1] != 5
    }
}
