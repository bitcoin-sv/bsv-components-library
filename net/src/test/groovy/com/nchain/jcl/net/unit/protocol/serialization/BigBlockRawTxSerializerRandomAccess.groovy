package com.nchain.jcl.net.unit.protocol.serialization

import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.PartialBlockRawTxMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.serialization.BlockHeaderMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.net.protocol.serialization.largeMsgs.RawBigBlockDeserializer
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayReaderRealTime
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Utils
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * A testing class to test the BigBlockRawDeserializer.
 * That Deserializer makes use of random access methods in the ByteReader, which can access specific bytes in the reader
 * given an offset, without consuming them. This allows for deserializing a Raw Tx faster: Instead of reading all the
 * fields, we just access directly those fields that help us find out the total length of
 * the Tx, and when we have it we just read (and consume) all the bytes .
 */
class BigBlockRawTxSerializerRandomAccess extends Specification {

    /**
     * We create a set of Several DUMMY Txs, and we check that the Number of TXs Deserialized is correct
     */
    def "testing Deserializing Raw Txs"() {
        when:
            // Some parameters to control how the DUMMY Txs are built. All the Txs are EQUAL:
            final int NUM_TXS = 100

            // for Each Tx:
            final int NUM_INPUTS = 300
            final int NUM_OUTPUTS = 100

            // For each Input/Output:
            final int INPUTS_SCRIPT_SIZE = 300
            final int OUTPUTS_SCRIPT_SIZE = 200

            // We create a Tx Object using the parameters above. Later on we'll Serialize it several times (NUM_TXS times)
            // so we generate a HEX String that wil be used as an Input for out Deserializer...

            // Dummy HASH:
            HashMsg zeroHashMsg = new HashMsg.HashMsgBuilder().hash(new byte[32]).build()
            // Dummy Outpoint:
            TxOutPointMsg txOutPointMsg = new TxOutPointMsg.TxOutPointMsgBuilder().hash(zeroHashMsg).index(1).build()
            // Tx Inputs:
            List<TxInputMsg> inputMsgs = new ArrayList<>()
            for (int i = 0; i < NUM_INPUTS; i++) {
                TxInputMsg txInputMsg = new TxInputMsg.TxInputMsgBuilder().pre_outpoint(txOutPointMsg).sequence(1).signature_script(new byte[INPUTS_SCRIPT_SIZE]).build()
                inputMsgs.add(txInputMsg)
            }
            // Tx Outputs:
            List<TxOutputMsg> outputsMsgs = new ArrayList<>()
            for (int i = 0; i < NUM_OUTPUTS; i++) {
                TxOutputMsg txOutputMsg = new TxOutputMsg.TxOutputMsgBuilder().txValue(2).pk_script(new byte[OUTPUTS_SCRIPT_SIZE]).build()
                outputsMsgs.add(txOutputMsg)
            }

            // Tx:
            TxMsg txMsg = new TxMsg.TxMsgBuilder()
                        .lockTime(1)
                        .version(1)
                        .tx_in(inputMsgs)
                        .tx_out(outputsMsgs)
                        .build()

            // WE serialze the same TX several times (NUM_TXS) and we get the HEX as a result:
            SerializerContext serContext = new SerializerContext.SerializerContextBuilder().build()
            ByteArrayWriter txByteWriter = new ByteArrayWriter()
            for (int i = 0; i < NUM_TXS; i++) {
                TxMsgSerializer.getInstance().serialize(serContext, txMsg, txByteWriter)
            }

            byte[] txsBytes = txByteWriter.reader().getFullContent()
            String txsHex = Utils.HEX.encode(txsBytes)

            println("Size of each Tx: " + txMsg.getLengthInBytes() + " bytes");
            println("Size of ALL the Txs: " + txsBytes.length + " bytes");

            // The HEX calculated previously still needs the Block Header before that. Now that we have the Txs already
            // Serialized, we use them to populate the block Header:

            // we serialize a Block Header:
            BlockHeaderMsg blockHeaderMsg = new BlockHeaderMsg.BlockHeaderMsgBuilder()
                .version(1)
                .hash(zeroHashMsg)
                .creationTimestamp(1)
                .difficultyTarget(1)
                .merkleRoot(zeroHashMsg)
                .nonce(1)
                .prevBlockHash(zeroHashMsg)
                .transactionCount(NUM_TXS)
                .build()

            ByteArrayWriter headerByteWriter = new ByteArrayWriter()
            BlockHeaderMsgSerializer.getInstance().serialize(serContext, blockHeaderMsg, headerByteWriter)
            byte[] headerBytes = headerByteWriter.reader().getFullContent()
            String headerHex = Utils.HEX.encode(headerBytes)

            // And the WHOLE HEX String that we'll use as an input is this:
            String BIG_BLOCK_HEX = headerHex + txsHex

            // Now we prepare for deserializing it using the BigBlockRawTxDeserializer:
            RawBigBlockDeserializer bigBlockDeserializer = new RawBigBlockDeserializer()
            bigBlockDeserializer.setPartialMsgSize(1_000_000) // 1 MB
            DeserializerContext desContext = new DeserializerContext.DeserializerContextBuilder()
                .maxBytesToRead(headerBytes.length + txsBytes.length)
                .build()

            // We keep track of how many Txs are deserialzied:
            AtomicReference<Long> numTxsDeserialized = new AtomicReference<>(0)

            // We are using A RealTimeReader, the one used for Bigblocks:
            ByteArrayReader byteReader = new ByteArrayReaderRealTime(Utils.HEX.decode(BIG_BLOCK_HEX))

            // When some Txs are deserialized and pushed back, we keep track of the number we get:
            bigBlockDeserializer.onDeserialized({ partialBlockMsg ->
                if (partialBlockMsg.data instanceof PartialBlockRawTxMsg) {
                    println("Received " + partialBlockMsg.data + " with " + ((PartialBlockRawTxMsg) partialBlockMsg.data).getTxs().size() + " txs...");
                    numTxsDeserialized.set(numTxsDeserialized.get() + ((PartialBlockRawTxMsg) partialBlockMsg.data).getTxs().size())
                }
            })

            // We deserialize and wait a bit:
            bigBlockDeserializer.deserialize(desContext, byteReader)
            Thread.sleep(5000)
        then:
            // We ONLY check that number of TXS (It might be worth it checking their content as well)
            numTxsDeserialized.get() == NUM_TXS
    }
}
