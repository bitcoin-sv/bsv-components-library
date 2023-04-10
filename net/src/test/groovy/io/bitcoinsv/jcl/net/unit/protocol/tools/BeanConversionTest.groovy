package io.bitcoinsv.jcl.net.unit.protocol.tools

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg
import io.bitcoinsv.jcl.net.protocol.serialization.BlockHeaderMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.TxMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.jcl.tools.common.TestingUtils
import spock.lang.Specification

/**
 * Test scenarios for the "toBean()" and "fromBean" methods in some Msg classes
 */
class BeanConversionTest extends Specification {

    /**
     * We test that a TxBean can be converted to a TxMsg and back again to a TxBean, using serialization in the middle,
     * and the TxBean result is equals to the original one.
     */
    def "testing Tx Conversion"() {
        given:
            Tx tx = TestingUtils.buildTx()
        when:
            TxMsg txMsg = TxMsg.fromBean(tx)
            SerializerContext serializerContext = new SerializerContext.SerializerContextBuilder().build()
            ByteArrayWriter writer = new ByteArrayWriter()
            TxMsgSerializer.getInstance().serialize(serializerContext, txMsg, writer)
            byte[] txMsgBytes = writer.reader().getFullContentAndClose()
            DeserializerContext desContext = new DeserializerContext.DeserializerContextBuilder().build()
            TxMsg txMsgDes = TxMsgSerializer.getInstance().deserialize(desContext, new ByteArrayReader(txMsgBytes))
            Tx txDes = txMsgDes.toBean()
        then:
            tx.equals(txDes)
    }

    /**
     * We test that a HeaderBean can be converted to a BlockHeaderNsg and back again to a HeaderBean,
     * using serialization in the middle, and the TxBean result is equals to the original one.
     */
    def "testing BlockHeader Conversion"() {
        given:
            final int NUM_TXS = 10
            // We create a Dummy Block Header out of a Bean:
            // Bean:
            HeaderBean blockHeader = TestingUtils.buildBlock()
            // Msg:
            BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.fromBean(blockHeader, NUM_TXS);

        when:
            SerializerContext serializerContext = new SerializerContext.SerializerContextBuilder().build()
            ByteArrayWriter writer = new ByteArrayWriter()
            BlockHeaderMsgSerializer.getInstance().serialize(serializerContext, blockHeaderMsg, writer)
            byte[] blockHeaderMsgBytes = writer.reader().getFullContentAndClose()
            DeserializerContext desContext = new DeserializerContext.DeserializerContextBuilder().build()
            BlockHeaderMsg blockHeaderMsgDes = BlockHeaderMsgSerializer.getInstance().deserialize(desContext, new ByteArrayReader(blockHeaderMsgBytes))
            HeaderBean headerDes = blockHeaderMsgDes.toBean()

        then:
            blockHeader.equals(headerDes)
    }

}
