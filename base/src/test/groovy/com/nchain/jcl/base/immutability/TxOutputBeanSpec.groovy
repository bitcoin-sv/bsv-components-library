package com.nchain.jcl.base.immutability

import com.nchain.jcl.base.core.Coin
import com.nchain.jcl.base.domain.api.base.TxOutPoint
import com.nchain.jcl.base.domain.api.base.TxOutput
import com.nchain.jcl.base.domain.bean.base.TxOutputBean
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class TxOutputBeanSpec extends Specification {

    def "creating instance and change original values"() {
        given:
            Coin value = Coin.FIFTY_COINS
            byte[] script = new byte[10]
            TxOutput bean = TxOutput.builder()
                    .value(value)
                    .scriptBytes(script)
                    .build()
        when:
            script[1] = 5
        then:
            bean.getScriptBytes()[1] != script[1]
    }

    def "getting scriptBytes adn change value"() {
        given:
            Coin value = Coin.FIFTY_COINS
            byte[] script = new byte[10]
            TxOutput bean = TxOutput.builder()
                .value(value)
                .scriptBytes(script)
                .build()
        when:
            bean.getScriptBytes()[1] = 5
        then:
            bean.getScriptBytes()[1] != 5
    }
}
