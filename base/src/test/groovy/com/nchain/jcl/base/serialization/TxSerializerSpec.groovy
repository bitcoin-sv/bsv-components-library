package com.nchain.jcl.base.serialization

import com.nchain.jcl.base.core.Coin
import com.nchain.jcl.base.domain.api.base.Tx
import com.nchain.jcl.base.domain.api.base.TxInput
import com.nchain.jcl.base.domain.api.base.TxOutput
import com.nchain.jcl.base.domain.bean.base.TxBean
import com.nchain.jcl.base.domain.bean.base.TxInputBean
import com.nchain.jcl.base.domain.bean.base.TxOutPointBean
import com.nchain.jcl.base.domain.bean.base.TxOutputBean
import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import spock.lang.Specification

class TxSerializerSpec extends Specification {

    // A TX Serialized, used as a reference:
    // - 1 Input
    // - 2 Outputs with values 179123 and 463000
    private static final String REF_TX =
            "010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a" +
            "0000006b483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e60" +
            "4329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2fa5d3494018" +
            "0121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3ffffff" +
            "ff02b3bb0200000000001976a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac9810" +
            "0700000000001976a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac00000000"

    private static final int TX_VERSION = 1
    private static final int TX_NUM_INPUTS = 1
    private static final int TX_NUM_OUTPUTS = 2

    // Sequence field for the input 0:
    private static final long INPUT_0_SEQUENCE = 4294967295

    // Values for the Input:
    private static final byte[] INPUT_0_SIG_SCRIPT  = HEX.decode(
            "483045022100af501dc9ef2907247d28a5169b8362ca494e1993f833928b77264e6" +
            "04329eec40220313594f38f97c255bcea6d5a4a68e920508ef93fd788bcf5b0ad2f" +
            "a5d34940180121034bb555cc39ba30561793cf39a35c403fe8cf4a89403b02b51e058960520bd1e3")

    // This is the Hash of the Outpoint of the first Inut. This hash is ALREADY REVERSED (human.readable order)
    private static final byte[] INPUT_0_OUTPOINT_HASH = HEX.decode("b3f912efba187b54cddb4202168c7d5fbafdfdce7c283d7ef1271dcc3e07e393")
    // Inpit outpoint Index
    private static final int INPUT_0_OUTPOINT_INDEX = 26

    // Value of the outputs:
    private static final int OUTPUT_0_VALUE = 179123
    private static final int OUTPUT_1_VALUE = 463000
    private static final byte[] OUTPUT_0_SCRIPT = HEX.decode("76a914f7d52018971f4ab9b56f0036958f84ae0325ccdc88ac")
    private static final byte[] OUTPUT_1_SCRIPT = HEX.decode("76a914f230f0a16a98433eca0fa70487b85fb83f7b61cd88ac")

    def "Deserializing TX"() {
        when:
            Tx tx = BitcoinSerializerFactory.deserialize(Tx.class, REF_TX);
        then:
            tx.getInputs().size() == TX_NUM_INPUTS
            tx.getOutputs().size() == TX_NUM_OUTPUTS
            tx.getVersion() == TX_VERSION
            tx.getInputs().get(0).sequenceNumber == INPUT_0_SEQUENCE
            tx.getInputs().get(0).scriptBytes  == INPUT_0_SIG_SCRIPT
            tx.getInputs().get(0).outpoint.hash.getBytes() == INPUT_0_OUTPOINT_HASH
            tx.getInputs().get(0).outpoint.index == INPUT_0_OUTPOINT_INDEX
            tx.getOutputs().get(0).value.value == OUTPUT_0_VALUE
            tx.getOutputs().get(1).value.value == OUTPUT_1_VALUE
            tx.getOutputs().get(0).scriptBytes == OUTPUT_0_SCRIPT
            tx.getOutputs().get(1).scriptBytes == OUTPUT_1_SCRIPT

    }

    def "serializing TX"() {
        when:
            TxInput input = TxInputBean.builder()
                  .outpoint(TxOutPointBean.builder()
                           .index(INPUT_0_OUTPOINT_INDEX)
                           .hash(Sha256Wrapper.wrap(INPUT_0_OUTPOINT_HASH))
                           .build())
                   .scriptBytes(INPUT_0_SIG_SCRIPT)
                   .sequenceNumber(INPUT_0_SEQUENCE)
                   .build()

            TxOutput output0 = TxOutputBean.builder().scriptBytes(OUTPUT_0_SCRIPT).value(Coin.valueOf(OUTPUT_0_VALUE))
                            .build()

            TxOutput output1 = TxOutputBean.builder().scriptBytes(OUTPUT_1_SCRIPT).value(Coin.valueOf(OUTPUT_1_VALUE))
                            .build()

            Tx tx = TxBean.builder()
                .version(TX_VERSION)
                .inputs(Arrays.asList(input))
                .outputs(Arrays.asList(output0, output1))
                .build()

            String tx_hex = HEX.encode(BitcoinSerializerFactory.serialize(tx))

        then:
            tx_hex.equals(REF_TX)
    }

    def "deserializing TX from Builder"() {
        when:
            Tx tx = Tx.builder(REF_TX).build()
        then:
            tx.getInputs().size() == TX_NUM_INPUTS
            tx.getOutputs().size() == TX_NUM_OUTPUTS
            tx.getVersion() == TX_VERSION
            tx.getInputs().get(0).sequenceNumber == INPUT_0_SEQUENCE
            tx.getInputs().get(0).scriptBytes  == INPUT_0_SIG_SCRIPT
            tx.getInputs().get(0).outpoint.hash.getBytes() == INPUT_0_OUTPOINT_HASH
            tx.getInputs().get(0).outpoint.index == INPUT_0_OUTPOINT_INDEX
            tx.getOutputs().get(0).value.value == OUTPUT_0_VALUE
            tx.getOutputs().get(1).value.value == OUTPUT_1_VALUE
            tx.getOutputs().get(0).scriptBytes == OUTPUT_0_SCRIPT
            tx.getOutputs().get(1).scriptBytes == OUTPUT_1_SCRIPT
    }

}
