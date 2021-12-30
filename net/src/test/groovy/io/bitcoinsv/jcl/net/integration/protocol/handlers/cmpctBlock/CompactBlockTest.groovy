package io.bitcoinsv.jcl.net.integration.protocol.handlers.cmpctBlock

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.*
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetBlockTxnMsg
import io.bitcoinsv.jcl.net.protocol.messages.GetdataMsg
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg
import io.bitcoinsv.jcl.net.protocol.messages.HeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.InvMessage
import io.bitcoinsv.jcl.net.protocol.messages.InventoryVectorMsg
import io.bitcoinsv.jcl.net.protocol.messages.SendCompactBlockMsg
import io.bitcoinsv.jcl.net.protocol.messages.SendHeadersMsg
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(20))
                .maxPeers(OptionalInt.of(25))
                .protocolVersion(70015)
                .build()

            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .build()

            Set<PeerAddress> peers = new HashSet<>()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicBoolean gotMissingTransactions = new AtomicBoolean()
            Set<String> blocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> invBlocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> headerBlocksReceived = Collections.synchronizedSet(new HashSet<>())

            AtomicReference<CompactBlockMsg> lastBlock = new AtomicReference<>()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                /**
                 * request peer to send us headers instead of inv messages
                 */
                SendHeadersMsg sendHeadersMsg = SendHeadersMsg.builder()
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, sendHeadersMsg).submit()

                /**
                 * send sendCmpct so we tell peer we want to get cmpctBlocks
                 * bandwidth mode is high so we get cmpctBlock directly without annunciation
                 */
                SendCompactBlockMsg msg = SendCompactBlockMsg.builder()
                    .highBandwidthRelaying(false)
                    .version(1)
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()

                /**
                 * we send out headers so we tell other node we are at the tip of the chain
                 */
                if (lastBlock.get() != null) {
                    CompactBlockHeaderMsg header = lastBlock.getHeader()

                    HeadersMsg headersMsg = HeadersMsg.builder()
                        .blockHeaderMsgList(List.of(
                                BlockHeaderMsg.builder()
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

                InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == InventoryVectorMsg.VectorType.MSG_BLOCK
                        || item.getType() == InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {

                        HashMsg hash = item.getHashMsg()
                        String hashString = Utils.HEX.encode(hash.getHashBytes())

                        invBlocksReceived.add(hashString)

                        /**
                         * we request cmpctBlock of first
                         */
                        GetdataMsg msg = GetdataMsg.builder()
                            .invVectorList(List.of(
                                InventoryVectorMsg.builder()
                                    .type(InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
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

                for (BlockHeaderMsg item : e.getBtcMsg().getBody().getBlockHeaderMsgList()) {
                    String hashString = Utils.HEX.encode(item.getHash().getHashBytes())
                    headerBlocksReceived.add(hashString)

                    GetdataMsg msg = GetdataMsg.builder()
                        .invVectorList(List.of(
                            InventoryVectorMsg.builder()
                                .type(InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
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

                CompactBlockMsg msg = e.getBtcMsg().getBody()
                CompactBlockHeaderMsg header = msg.getHeader()
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
                HeadersMsg headersMsg = HeadersMsg.builder()
                    .blockHeaderMsgList(List.of(
                        BlockHeaderMsg.builder()
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

                for (PeerAddress peer : peers) {
                    p2p.REQUESTS.MSGS.send(peer, headersMsg).submit()
                }

                GetBlockTxnMsg getBlockTxnMsg = GetBlockTxnMsg.builder()
                    .blockHash(header.getHash())
                    .indexesLength(VarIntMsg.builder().value(5).build())
                    .indexes(List.of(
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build()
                    ))
                    .build()

                p2p.REQUESTS.MSGS.send(e.peerAddress, getBlockTxnMsg).submit()

            })

            p2p.EVENTS.MSGS.GETDATA.forEach({ e ->
                println("GETDATA message received")

                for (InventoryVectorMsg vector : e.getBtcMsg().getBody().getInvVectorList()) {
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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(20))
                .maxPeers(OptionalInt.of(25))
                .protocolVersion(70015)
                .build()

            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .build()

            Set<PeerAddress> peers = new HashSet<>()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicBoolean gotMissingTransactions = new AtomicBoolean()
            Set<String> blocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> invBlocksReceived = Collections.synchronizedSet(new HashSet<>())

            AtomicReference<CompactBlockMsg> lastBlock = new AtomicReference<>()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                /**
                 * request peer to send us headers instead of inv messages
                 */
                SendHeadersMsg sendHeadersMsg = SendHeadersMsg.builder()
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, sendHeadersMsg).submit()

                /**
                 * send sendCmpct so we tell peer we want to get cmpctBlocks
                 * bandwidth mode is high so we get cmpctBlock directly without annunciation
                 */
                SendCompactBlockMsg msg = SendCompactBlockMsg.builder()
                    .highBandwidthRelaying(true)
                    .version(1)
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()

                if (lastBlock.get() != null) {
                    CompactBlockHeaderMsg header = lastBlock.getHeader()

                    /**
                     * we send out headers so we tell other node we are at the tip of the chain
                     */
                    HeadersMsg headersMsg = HeadersMsg.builder()
                        .blockHeaderMsgList(List.of(
                            BlockHeaderMsg.builder()
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

                InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == InventoryVectorMsg.VectorType.MSG_BLOCK
                        || item.getType() == InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {

                        HashMsg hash = item.getHashMsg()
                        String hashString = Utils.HEX.encode(hash.getHashBytes())

                        invBlocksReceived.add(hashString)

                        /**
                         * we request cmpctBlock of first
                         */
                        GetdataMsg msg = GetdataMsg.builder()
                            .invVectorList(List.of(
                                InventoryVectorMsg.builder()
                                    .type(InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
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

                CompactBlockMsg msg = e.getBtcMsg().getBody()
                CompactBlockHeaderMsg header = msg.getHeader()
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
                HeadersMsg headersMsg = HeadersMsg.builder()
                    .blockHeaderMsgList(List.of(
                        BlockHeaderMsg.builder()
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

                for (PeerAddress peer : peers) {
                    p2p.REQUESTS.MSGS.send(peer, headersMsg).submit()
                }

                /**
                 * we request first 5 transactions from the block
                 */
                GetBlockTxnMsg getBlockTxnMsg = GetBlockTxnMsg.builder()
                    .blockHash(header.getHash())
                    .indexesLength(VarIntMsg.builder().value(5).build())
                    .indexes(List.of(
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build(),
                        VarIntMsg.builder().value(0).build()
                    ))
                    .build()

                p2p.REQUESTS.MSGS.send(e.peerAddress, getBlockTxnMsg).submit()

            })

            p2p.EVENTS.MSGS.GETDATA.forEach({ e ->
                println("GETDATA message received")

                for (InventoryVectorMsg vector : e.getBtcMsg().getBody().getInvVectorList()) {
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
