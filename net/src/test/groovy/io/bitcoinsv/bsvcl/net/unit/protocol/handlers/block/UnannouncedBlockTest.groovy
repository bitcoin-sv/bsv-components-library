package io.bitcoinsv.bsvcl.net.unit.protocol.handlers.block


import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.HeaderBean
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.params.RegTestParams
import io.bitcoinsv.bsvcl.common.common.TestingUtils
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Test scenario where 2 instances of JCL-Net connect to each other and exchange a Block, WITHOUT requesting that
 * Block first. the Block should come through, since in the current implementation all Peers are allowed to accept
 * unannounced blocks and with no size limits.
 */
class UnannouncedBlockTest extends Specification {

    // It creates a DUMMY BLOCK:
    private io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> createDummyBlock(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig, int numTxs) {
        HeaderBean blockHeader = TestingUtils.buildBlock()
        io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg blockHeaderMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.fromBean(blockHeader, numTxs);
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg> txsMsg = IntStream.range(0, numTxs)
                .mapToObj({i -> TestingUtils.buildTx()})
                .map({ tx -> io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg.fromBean(tx)})
                .collect(Collectors.toList())

        io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg blockMsg = io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg.builder()
                .blockHeader(blockHeaderMsg)
                .transactionMsgs(txsMsg)
                .build()

        io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> result = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(protocolConfig.basicConfig, blockMsg).build()
        return result;
    }


    /**
     * this configures 2 instances of JCL: A Server and a CLIENT, connect them together and then send a DUMMY BLOCK
     * from the client ot the Server. When the Block is received at the Server end, we save the reference to its hash
     * and we return it when the method ends.
     *
     * The parameters allows ut to control if we want the Block to be considered a "Big" Block or a "regular" block.
     *
     * @param minBytesForRealTimeProcessing threshold above which MSgs are treated as "Big" messages
     * @param blockMsg                      Dummy Block sent from the client to the Server
     * @param hashBlockReceived             parameter used to store result: after execution it will contain the
     *                                      hash of the block received.
     */
    private void sendBlockFromClientToServer(int minBytesForRealTimeProcessing,
                                             io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> blockMsg,
                                             AtomicReference<Sha256Hash> hashBlockReceived) {
        // RuntimeConfig
        RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .msgSizeInBytesForRealTimeProcessing(minBytesForRealTimeProcessing)
                .build()

        // We set up the Protocol configuration
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new RegTestParams()).toBuilder()
                .minPeers(1)
                .maxPeers(1)
                .build()

        // For the "server", we allow ALL the Peers to send "Big" Messages:
        io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig msgConfig = config.getMessageConfig().toBuilder()
                .allowBigMsgFromAllPeers(true)
                .build()
        io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P server = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("server")
                .config(runtimeConfig)                              // "Big" Msgs workaround
                .config(config)
                .config(msgConfig)                                  // Allow "Big" msgs from ALL Peers
                .serverPort(0)                            // random port
                .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler.HANDLER_ID)        // No blacklist functionality
                .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler.HANDLER_ID)        // No Discovery functionality
                .build()
        io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P client = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("client")
                .config(config)
                .serverPort(0)                            // random port
                .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler.HANDLER_ID)        // No blacklist functionality
                .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler.HANDLER_ID)        // No Discovery functionality
                .build()


        // Event handler: Triggered when a Whole block has been downloaded by the Server
        server.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
            println("Block downloaded #" + e.blockHeader.hash)
            hashBlockReceived.set(e.blockHeader.hash)
        })

        // We start Server and Client and connect each other:
        server.startServer()
        client.start()
        println(" > Client started: " + client.getPeerAddress())
        println(" > Server started: " + server.getPeerAddress())

        // We wait a bit and connect each other:
        Thread.sleep(500)
        println("> Client Connecting to Server...")
        client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

        // We Wait a bit and send a BLOCK from The Client to the Server...
        Thread.sleep(1000)
        println("Sending Block #" + blockMsg.body.blockHeader.hash + "...")
        client.REQUESTS.MSGS.send(server.getPeerAddress(), blockMsg).submit()

        // We Wait a bit an check the Events have been propagated properly...
        Thread.sleep(1000)

        // And we stop
        client.stop()
        server.stop()

    }


    /**
     * We test that we can send an UNANNOUNCED "regular" block (not a "Big" block) to JCL
     */
    def "Testing Regular Block"() {
        given:
            // Protocol Config:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new RegTestParams()).toBuilder()
                .minPeers(1)
                .maxPeers(1)
                .build()

            // Num of Tx in the Block:
            final int NUM_TXS_IN_BLOCK = 10
            final int NUM_BYTES_BIG_MSGS = 50000
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> blockMsg = createDummyBlock(config, NUM_TXS_IN_BLOCK)
            AtomicReference<Sha256Hash> hashBlockReceived = new AtomicReference<>()
        when:
            sendBlockFromClientToServer(NUM_BYTES_BIG_MSGS, blockMsg, hashBlockReceived)
        then:
            blockMsg.getBody().getBlockHeader().getHash().equals(hashBlockReceived.get())
    }

    /**
     * We test that we can send an UNANNOUNCED "Big" block to JCL
     */
    def "Testing Big Block"() {
        given:
            // Protocol Config:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new RegTestParams()).toBuilder()
                    .minPeers(1)
                    .maxPeers(1)
                    .build()

            // Num of Tx in the Block:
            final int NUM_TXS_IN_BLOCK = 10
            final int NUM_BYTES_BIG_MSGS = 500
            io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg> blockMsg = createDummyBlock(config, NUM_TXS_IN_BLOCK)
            AtomicReference<Sha256Hash> hashBlockReceived = new AtomicReference<>()
        when:
            sendBlockFromClientToServer(NUM_BYTES_BIG_MSGS, blockMsg, hashBlockReceived)
        then:
            blockMsg.getBody().getBlockHeader().getHash().equals(hashBlockReceived.get())
    }
}
