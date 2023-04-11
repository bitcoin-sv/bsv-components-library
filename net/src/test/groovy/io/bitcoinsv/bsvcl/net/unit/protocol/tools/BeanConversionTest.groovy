package io.bitcoinsv.bsvcl.net.unit.protocol.tools

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bsvcl.common.common.TestingUtils
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
            io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg txMsg = io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg.fromBean(tx)
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext serializerContext = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.SerializerContextBuilder().build()
            ByteArrayWriter writer = new ByteArrayWriter()
            io.bitcoinsv.bsvcl.net.protocol.serialization.TxMsgSerializer.getInstance().serialize(serializerContext, txMsg, writer)
            byte[] txMsgBytes = writer.reader().getFullContentAndClose()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext desContext = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.DeserializerContextBuilder().build()
            io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg txMsgDes = io.bitcoinsv.bsvcl.net.protocol.serialization.TxMsgSerializer.getInstance().deserialize(desContext, new ByteArrayReader(txMsgBytes))
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
            io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg blockHeaderMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.fromBean(blockHeader, NUM_TXS);

        when:
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext serializerContext = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.SerializerContextBuilder().build()
            ByteArrayWriter writer = new ByteArrayWriter()
            io.bitcoinsv.bsvcl.net.protocol.serialization.BlockHeaderMsgSerializer.getInstance().serialize(serializerContext, blockHeaderMsg, writer)
            byte[] blockHeaderMsgBytes = writer.reader().getFullContentAndClose()
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext desContext = new io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.DeserializerContextBuilder().build()
            io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg blockHeaderMsgDes = io.bitcoinsv.bsvcl.net.protocol.serialization.BlockHeaderMsgSerializer.getInstance().deserialize(desContext, new ByteArrayReader(blockHeaderMsgBytes))
            HeaderBean headerDes = blockHeaderMsgDes.toBean()

        then:
            blockHeader.equals(headerDes)
    }

}
