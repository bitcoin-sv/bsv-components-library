package io.bitcoinsv.jcl.net.unit.network.streams

import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.TxOutPoint
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxInputMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxOutPointMsg
import io.bitcoinsv.jcl.net.protocol.messages.TxOutputMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.StreamRequest
import io.bitcoinsv.jcl.net.protocol.serialization.BlockMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter

import spock.lang.Specification

import java.util.stream.Stream

/**
 * Testing class to check the new streaming functionality and whether two nodes are able to communicate with each other using
 * the streaming interface.
 */
class BlockStreamTest extends Specification {


    def "Testing Client/Server can stream blocks"() {
        given:
        // We disable the Handlers we dont need for this Test:
        ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        // Reduce ext msg size to 10KB from 4GB for the test, and enable ext msgs using later protocol version
        ProtocolBasicConfig basicConfig = protocolConfig.basicConfig.toBuilder()
                .thresholdSizeExtMsgs(10_000)
                .protocolVersion(ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build()
        //update config with new settings
        protocolConfig = protocolConfig.toBuilder().basicConfig(basicConfig).build();

        P2P server = new P2PBuilder("server")
                .config(protocolConfig)
                .serverPort(0) // Random Port
                .excludeHandler(PingPongHandler.HANDLER_ID)
                .excludeHandler(DiscoveryHandler.HANDLER_ID)
                .excludeHandler(BlacklistHandler.HANDLER_ID)
                .build()

        // We disable the Handlers we dont need for this Test:
        P2P client = new P2PBuilder("client")
                .config(protocolConfig)
                .excludeHandler(PingPongHandler.HANDLER_ID)
                .excludeHandler(DiscoveryHandler.HANDLER_ID)
                .excludeHandler(BlacklistHandler.HANDLER_ID)
                .build()

        int totalTxsInBlock = 1000

        when:
        server.startServer()
        client.start()

        Thread.sleep(1000)
        client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
        Thread.sleep(1000)

        //listen for block received on the server
        BlockMsg receivedBlockMsg = null;
        server.EVENTS.MSGS.BLOCK.forEach({ e ->
            receivedBlockMsg = e.getBtcMsg().getBody()
        })

        //generate a random block
        HeaderReadOnly block = TestingUtils.buildBlock()

        //create a list of txs to add to the block
        List<TxMsg> txMsgList = new ArrayList<>()
        for(int i = 0; i < totalTxsInBlock; i++){
            Tx tx = TestingUtils.buildTx()

            //construct the TxInputMsgs from the generated tx
            List<TxInputMsg> txInputMsgs = new ArrayList<>()
            tx.getInputs().forEach({ txInput ->
                TxOutPoint outPoint = txInput.getOutpoint()
                TxOutPointMsg txOutPointMsg = new TxOutPointMsg(HashMsg.builder().hash(outPoint.getHash().getReversedBytes()).build(), outPoint.getIndex())

                TxInputMsg txInputMsg = new TxInputMsg(txOutPointMsg, txInput.getScriptBytes(), txInput.getSequenceNumber())
                txInputMsgs.add(txInputMsg)
            })

            //construct the TxOutputMsgs for the generated tx
            List<TxOutputMsg> txOutputMsgs = new ArrayList<>()
            tx.getOutputs().forEach({ txOutput ->
                TxOutputMsg txOutputMsg = new TxOutputMsg(txOutput.getValue().getValue(), txOutput.getScriptBytes());
                txOutputMsgs.add(txOutputMsg)
            })

            //create the tx msg
            TxMsg txMsg = new TxMsg(Optional.empty(), tx.getVersion(), txInputMsgs, txOutputMsgs, tx.getLockTime(), new byte[0], 0)

            //add to list of tx msgs within block
            txMsgList.add(txMsg)
        }

        //build the block header
        BlockHeaderMsg blockHeaderMsg = BlockHeaderMsg.builder()
                .difficultyTarget(block.getDifficultyTarget())
                .creationTimestamp(block.getTime())
                .prevBlockHash(HashMsg.builder().hash(block.getPrevBlockHash().getReversedBytes()).build())
                .merkleRoot(HashMsg.builder().hash(block.getMerkleRoot().getReversedBytes()).build())
                .nonce(block.getNonce())
                .version(block.getVersion())
                .transactionCount(txMsgList.size())
                .difficultyTarget(block.getDifficultyTarget())
                .build()


        //construct the block msg
        BlockMsg blockMsg = BlockMsg.builder().blockHeader(blockHeaderMsg).transactionMsgs(txMsgList).build();

        //load the serializers
        SerializerContext context = SerializerContext.builder()
                .protocolBasicConfig(protocolConfig.getBasicConfig())
                .build()
        BlockMsgSerializer serializer = new BlockMsgSerializer();

        //serialize the block
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter()
        serializer.serialize(context, blockMsg, byteArrayWriter)
        byte[] blockBytes = byteArrayWriter.reader().getFullContentAndClose();

        //build the stream request and send down the pipeline
        StreamRequest streamRequest = new StreamRequest(BlockMsg.MESSAGE_TYPE, Stream.of(blockBytes), blockBytes.length)
        client.REQUESTS.MSGS.stream(server.getPeerAddress(), streamRequest).submit()

        //wait for the server to receive the request
        Thread.sleep(6000)

        //stop server as we no longer need it
        server.stop()
        client.stop()

        then:
        receivedBlockMsg != null
        //check the msg we sent is the same as the msg we received
        receivedBlockMsg.getBlockHeader().toBean() == blockMsg.getBlockHeader().toBean()

        for(int i = 0; i < totalTxsInBlock; i++) {
            receivedBlockMsg.getTransactionMsg().get(i).toBean() == blockMsg.getTransactionMsg().get(i).toBean()
        }

    }
}
