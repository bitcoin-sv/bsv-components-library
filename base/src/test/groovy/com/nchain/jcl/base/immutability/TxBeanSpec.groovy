package com.nchain.jcl.base.immutability

import com.nchain.jcl.base.core.Coin
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.domain.api.base.TxInput
import com.nchain.jcl.base.domain.api.base.TxOutput
import spock.lang.Specification

class TxBeanSpec extends Specification {

    def "creating instances and change original values"() {
        given:
            List<TxInput> inputs = new ArrayList<>()
            inputs.add(TxInput.builder().sequenceNumber(1).build())
            inputs.add(TxInput.builder().sequenceNumber(2).build())

            List<TxOutput> outputs = new ArrayList<>()
            outputs.add(TxOutput.builder().value(Coin.FIFTY_COINS).build())
            outputs.add(TxOutput.builder().value(Coin.FIFTY_COINS).build())

            Tx bean = Tx.builder()
                .inputs(inputs)
                .outputs(outputs)
                .build()
        when:
            inputs.remove(0)
            outputs.remove(0)
        then:
            bean.getInputs().size() == 2
            bean.getOutputs().size() == 2
    }

    def "getting inputs/outputs and change them"() {
        given:
            List<TxInput> inputs = new ArrayList<>()
            inputs.add(TxInput.builder().sequenceNumber(1).build())
            inputs.add(TxInput.builder().sequenceNumber(2).build())

            List<TxOutput> outputs = new ArrayList<>()
            outputs.add(TxOutput.builder().value(Coin.FIFTY_COINS).build())
            outputs.add(TxOutput.builder().value(Coin.FIFTY_COINS).build())

            Tx bean = Tx.builder()
                    .inputs(inputs)
                    .outputs(outputs)
                    .build()
        when:
            bean.getInputs().remove(0)
            bean.getOutputs().remove(0)
        then:
            thrown UnsupportedOperationException
    }
}
