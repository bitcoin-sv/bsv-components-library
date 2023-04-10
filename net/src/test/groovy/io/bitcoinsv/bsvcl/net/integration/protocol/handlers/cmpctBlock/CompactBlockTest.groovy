package io.bitcoinsv.bsvcl.net.integration.protocol.handlers.cmpctBlock


import io.bitcoinsv.jcl.net.protocol.messages.*
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * An integration Test for Downloading Blocks.
 */
class CompactBlockTest extends Specification {

    @Ignore
    def "Testing get compact block low bandwidth mode"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(20))
                .maxPeers(OptionalInt.of(25))
                .protocolVersion(70015)
                .build()

            // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
            io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("testing")
                .config(config)
                .config(discoveryConfig)
                .config(basicConfig)
                .build()

            Set<io.bitcoinsv.bsvcl.net.network.PeerAddress> peers = new HashSet<>()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicBoolean gotMissingTransactions = new AtomicBoolean()
            Set<String> blocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> invBlocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> headerBlocksReceived = Collections.synchronizedSet(new HashSet<>())

            AtomicReference<io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockMsg> lastBlock = new AtomicReference<>()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                /**
                 * request peer to send us headers instead of inv messages
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg sendHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg.builder()
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, sendHeadersMsg).submit()

                /**
                 * send sendCmpct so we tell peer we want to get cmpctBlocks
                 * bandwidth mode is high so we get cmpctBlock directly without annunciation
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg msg = io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.builder()
                    .highBandwidthRelaying(false)
                    .version(1)
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()

                /**
                 * we send out headers so we tell other node we are at the tip of the chain
                 */
                if (lastBlock.get() != null) {
                    io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockHeaderMsg header = lastBlock.getHeader()

                    io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg headersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg.builder()
                        .blockHeaderMsgList(List.of(
                                io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.builder()
                                .hash(header.getHash())
                                .version(header.getVersion())
                                .prevBlockHash(header.getPrevBlockHash())
                                .merkleRoot(header.getMerkleRoot())
                                .creationTimestamp(header.getCreationTimestamp())
                                .difficultyTarget(header.getDifficultyTarget())
                                .nonce(header.getNonce())
                                .transactionCount(0)
                                .build()
                        ))
                        .build()

                    p2p.REQUESTS.MSGS.send(e.getPeerAddress(), headersMsg).submit()
                }
            })

            /**
             * this is called only for the first new block where we are waiting to be synced with the tip
             */
            p2p.EVENTS.MSGS.INV.forEach({ e ->
                println("INV message received")

                io.bitcoinsv.bsvcl.net.protocol.messages.InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_BLOCK
                        || item.getType() == io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {

                        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hash = item.getHashMsg()
                        String hashString = Utils.HEX.encode(hash.getHashBytes())

                        invBlocksReceived.add(hashString)

                        /**
                         * we request cmpctBlock of first
                         */
                        io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg msg = io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg.builder()
                            .invVectorList(List.of(
                                io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.builder()
                                    .type(io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
                                    .hashMsg(item.getHashMsg())
                                    .build()
                            ))
                            .build()

                        // request compact block
                        p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                    }
                }
            })

            p2p.EVENTS.MSGS.SENDCMPCT_SENT.forEach({ e ->
                println("SENDCMPCT message sent: " + e.getBtcMsg().getBody().isHighBandwidthRelaying())
            })

            p2p.EVENTS.MSGS.SENDCMPCT.forEach({ e ->
                println("SENDCMPCT message received: " + e.getBtcMsg().getBody().isHighBandwidthRelaying())

                peers.add(e.getPeerAddress())
            })

            /**
             * this is called when we get the second block after other peers know we are at the tip of the chain
             */
            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                println("HEADERS message received")

                for (io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg item : e.getBtcMsg().getBody().getBlockHeaderMsgList()) {
                    String hashString = Utils.HEX.encode(item.getHash().getHashBytes())
                    headerBlocksReceived.add(hashString)

                    io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg msg = io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg.builder()
                        .invVectorList(List.of(
                            io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.builder()
                                .type(io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
                                .hashMsg(item.getHash())
                                .build()
                        ))
                        .build()

                    // request compact block
                    p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                }
            })

            p2p.EVENTS.MSGS.CMPCTBLOCK.forEach({ e ->
                println("CMPCTBLOCK message received")

                io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockMsg msg = e.getBtcMsg().getBody()
                io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockHeaderMsg header = msg.getHeader()
                String hashString = Utils.HEX.encode(header.getHash().getHashBytes())

                /**
                 * when we get fist inv message of new block we store it so we can tell other peers we are at the tip of the chain
                 */
                if (lastBlock.get() == null) {
                    lastBlock.set(msg)
                }

                blocksReceived.add(hashString)

                /**
                 * we notify other nodes we got new block
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg headersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg.builder()
                    .blockHeaderMsgList(List.of(
                        io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.builder()
                            .hash(header.getHash())
                            .version(header.getVersion())
                            .prevBlockHash(header.getPrevBlockHash())
                            .merkleRoot(header.getMerkleRoot())
                            .creationTimestamp(header.getCreationTimestamp())
                            .difficultyTarget(header.getDifficultyTarget())
                            .nonce(header.getNonce())
                            .transactionCount(0)
                            .build()
                    ))
                    .build()

                for (io.bitcoinsv.bsvcl.net.network.PeerAddress peer : peers) {
                    p2p.REQUESTS.MSGS.send(peer, headersMsg).submit()
                }

                io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg getBlockTxnMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg.builder()
                    .blockHash(header.getHash())
                    .indexesLength(io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(5).build())
                    .indexes(List.of(
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build()
                    ))
                    .build()

                p2p.REQUESTS.MSGS.send(e.peerAddress, getBlockTxnMsg).submit()

            })

            p2p.EVENTS.MSGS.GETDATA.forEach({ e ->
                println("GETDATA message received")

                for (io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg vector : e.getBtcMsg().getBody().getInvVectorList()) {
                    println("Requested GetData: " + vector.getType())
                }
            })

            p2p.EVENTS.MSGS.BLOCKTXN.forEach({ e ->
                println("BLOCKTXN message received")

                /**
                 * check if we got 5 requested transactions from the first block
                 */
                if (e.getBtcMsg().getBody().transactions.size() == 5) {
                    gotMissingTransactions.set(true)
                }
            })

        when:
            println(" > Testing get compact block message in " + config.toString() + "...")

            p2p.start()

            // wait long enough to new block be created
            while (blocksReceived.size() < 2) {
                Thread.sleep(1000)
            }

            p2p.stop()

        then:
            numOfHandshakes.get() > 0
            invBlocksReceived.size() == 1
            headerBlocksReceived.size() == 1
            blocksReceived.size() == 2
            gotMissingTransactions.get()
    }

    @Ignore
    def "Testing get compact block high bandwidth mode"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(20))
                .maxPeers(OptionalInt.of(25))
                .protocolVersion(70015)
                .build()

            // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
            io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("testing")
                .config(config)
                .config(discoveryConfig)
                .config(basicConfig)
                .build()

            Set<io.bitcoinsv.bsvcl.net.network.PeerAddress> peers = new HashSet<>()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicBoolean gotMissingTransactions = new AtomicBoolean()
            Set<String> blocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> invBlocksReceived = Collections.synchronizedSet(new HashSet<>())

            AtomicReference<io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockMsg> lastBlock = new AtomicReference<>()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                /**
                 * request peer to send us headers instead of inv messages
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg sendHeadersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.SendHeadersMsg.builder()
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, sendHeadersMsg).submit()

                /**
                 * send sendCmpct so we tell peer we want to get cmpctBlocks
                 * bandwidth mode is high so we get cmpctBlock directly without annunciation
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg msg = io.bitcoinsv.bsvcl.net.protocol.messages.SendCompactBlockMsg.builder()
                    .highBandwidthRelaying(true)
                    .version(1)
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()

                if (lastBlock.get() != null) {
                    io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockHeaderMsg header = lastBlock.getHeader()

                    /**
                     * we send out headers so we tell other node we are at the tip of the chain
                     */
                    io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg headersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg.builder()
                        .blockHeaderMsgList(List.of(
                            io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.builder()
                                .hash(header.getHash())
                                .version(header.getVersion())
                                .prevBlockHash(header.getPrevBlockHash())
                                .merkleRoot(header.getMerkleRoot())
                                .creationTimestamp(header.getCreationTimestamp())
                                .difficultyTarget(header.getDifficultyTarget())
                                .nonce(header.getNonce())
                                .transactionCount(0)
                                .build()
                        ))
                        .build()

                    p2p.REQUESTS.MSGS.send(e.getPeerAddress(), headersMsg).submit()
                }
            })

            /**
             * this is called only for the first new block where we are waiting to be synced with the tip
             */
            p2p.EVENTS.MSGS.INV.forEach({ e ->
                println("INV message received")

                io.bitcoinsv.bsvcl.net.protocol.messages.InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_BLOCK
                        || item.getType() == io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {

                        io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg hash = item.getHashMsg()
                        String hashString = Utils.HEX.encode(hash.getHashBytes())

                        invBlocksReceived.add(hashString)

                        /**
                         * we request cmpctBlock of first
                         */
                        io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg msg = io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg.builder()
                            .invVectorList(List.of(
                                io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.builder()
                                    .type(io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
                                    .hashMsg(item.getHashMsg())
                                    .build()
                            ))
                            .build()

                        // request compact block
                        p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                    }
                }
            })

            p2p.EVENTS.MSGS.SENDCMPCT_SENT.forEach({ e ->
                println("SENDCMPCT message sent: " + e.getBtcMsg().getBody().isHighBandwidthRelaying())
            })

            p2p.EVENTS.MSGS.SENDCMPCT.forEach({ e ->
                println("SENDCMPCT message received: " + e.getBtcMsg().getBody().isHighBandwidthRelaying())
                peers.add(e.getPeerAddress())
            })

            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                println("HEADERS message received")
                println("rec: " + e.getBtcMsg())
            })

            p2p.EVENTS.MSGS.CMPCTBLOCK.forEach({ e ->
                println("CMPCTBLOCK message received")

                io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockMsg msg = e.getBtcMsg().getBody()
                io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockHeaderMsg header = msg.getHeader()
                String hashString = Utils.HEX.encode(header.getHash().getHashBytes())

                /**
                 * when we get fist inv message of new block we store it so we can tell other peers we are at the tip of the chain
                 */
                if (lastBlock.get() == null) {
                    lastBlock.set(msg)
                }

                blocksReceived.add(hashString)

                /**
                 * we notify other nodes we got new block
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg headersMsg = io.bitcoinsv.bsvcl.net.protocol.messages.HeadersMsg.builder()
                    .blockHeaderMsgList(List.of(
                        io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg.builder()
                            .hash(header.getHash())
                            .version(header.getVersion())
                            .prevBlockHash(header.getPrevBlockHash())
                            .merkleRoot(header.getMerkleRoot())
                            .creationTimestamp(header.getCreationTimestamp())
                            .difficultyTarget(header.getDifficultyTarget())
                            .nonce(header.getNonce())
                            .transactionCount(0)
                            .build()
                    ))
                    .build()

                for (io.bitcoinsv.bsvcl.net.network.PeerAddress peer : peers) {
                    p2p.REQUESTS.MSGS.send(peer, headersMsg).submit()
                }

                /**
                 * we request first 5 transactions from the block
                 */
                io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg getBlockTxnMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetBlockTxnMsg.builder()
                    .blockHash(header.getHash())
                    .indexesLength(io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(5).build())
                    .indexes(List.of(
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build(),
                        io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg.builder().value(0).build()
                    ))
                    .build()

                p2p.REQUESTS.MSGS.send(e.peerAddress, getBlockTxnMsg).submit()

            })

            p2p.EVENTS.MSGS.GETDATA.forEach({ e ->
                println("GETDATA message received")

                for (io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg vector : e.getBtcMsg().getBody().getInvVectorList()) {
                    println("Requested GetData: " + vector.getType())
                }
            })

            p2p.EVENTS.MSGS.BLOCKTXN.forEach({ e ->
                println("BLOCKTXN message received")

                /**
                 * check if we got 5 requested transactions from the first block
                 */
                if (e.getBtcMsg().getBody().transactions.size() == 5) {
                    gotMissingTransactions.set(true)
                }
            })

        when:
            println(" > Testing get compact block message in " + config.toString() + "...")

            p2p.start()

            // wait long enough to new block be created
            while (blocksReceived.size() < 2) {
                Thread.sleep(1000)
            }

            p2p.stop()

        then:
            numOfHandshakes.get() > 0
            invBlocksReceived.size() == 1
            blocksReceived.size() > 1
            gotMissingTransactions.get()
    }
}
