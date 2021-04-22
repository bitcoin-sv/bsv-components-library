package com.nchain.jcl.net.integration.protocol.handlers.cmpctBlock

import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.messages.*
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * An integration Test for Downloading Blocks.
 */
class CompactBlockTest extends Specification {

    def "Testing get compact block low bandwidth mode"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(45))
                .maxPeers(OptionalInt.of(50))
                .protocolVersion(70015)
                .build()

            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .build()

            AtomicInteger numOfHandshakes = new AtomicInteger()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                if (e.versionMsg.version >= 70014) {
                    SendCompactBlockMsg msg = SendCompactBlockMsg.builder()
                        .highBandwidthRelaying(false)
                        .version(1)
                        .build()
                    p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                }
            })

            p2p.EVENTS.MSGS.SENDCMPCT_SENT.forEach({ e ->
                println("Send cmpct block message sent")
            })

            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                println("Headers message received")
            })

            p2p.EVENTS.MSGS.BLOCK.forEach({ e ->
                println("BLOCK message received")
            })

            p2p.EVENTS.MSGS.INV.forEach({ e ->
                println("Inv message received")

                InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == InventoryVectorMsg.VectorType.MSG_BLOCK
                        || item.getType() == InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {
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
                    } else {
                        println(item.getType())
                    }
                }
            })

            p2p.EVENTS.MSGS.SENDCMPCT.forEach({ e ->
                println("Send cmpct block message received: " + e.getBtcMsg().getBody().isHighBandwidthRelaying())
            })

            Map<String, GetBlockTxnMsg> requestedTransactions = new ConcurrentHashMap<>()

            AtomicInteger numberOfCompactBlockReceived = new AtomicInteger()
            p2p.EVENTS.MSGS.CMPCTBLOCK.forEach({ e ->
                println("Compact block message received")
                numberOfCompactBlockReceived.set(numberOfCompactBlockReceived.get() + 1)

                // request some transactions
                CompactBlockMsg compactBlockMsg = e.getBtcMsg().getBody()

                List<Long> randTxIdIndexes = new ArrayList<>()

                // request first 10 transactions as missing
                for (int i = 0; i < 10; i++) {
                    randTxIdIndexes.add(0)
                }

                GetBlockTxnMsg msg = GetBlockTxnMsg.builder()
                    .blockHash(compactBlockMsg.getHeader().getHash())
                    .indexesLength(VarIntMsg.builder().value(randTxIdIndexes.size()).build())
                    .indexes(
                        randTxIdIndexes.stream()
                            .map({ value -> VarIntMsg.builder().value(value).build() })
                            .collect(Collectors.toList())
                    )
                    .build()

                requestedTransactions.put(e.getPeerAddress().getIp().toString(), msg)

                // request missing transactions
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
            })

            AtomicBoolean transactionsReceived = new AtomicBoolean()

            p2p.EVENTS.MSGS.GETBLOCKTXN_SENT.forEach({ e ->
                println("GetBlockTxn message sent")
            })

            p2p.EVENTS.MSGS.BLOCKTXN.forEach({ e ->
                println("Blocktxn message received")

                GetBlockTxnMsg request = requestedTransactions.get(e.getPeerAddress().getIp().toString())
                BlockTxnMsg response = e.getBtcMsg().getBody()

                if (request.getIndexes().size() == response.getTransactions().size()) {
                    println("got all transactions")
                    transactionsReceived.set(true)
                }
            })

        when:
            println(" > Testing get compact block message in " + config.toString() + "...")

            p2p.start()

            // wait long enough to new block be created
            while (!transactionsReceived.get()) {
                Thread.sleep(1000)
            }

            p2p.stop()

        then:
            numOfHandshakes.get() > 0
            transactionsReceived.get()
    }

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

            Set<PeerAddress> peers = new HashSet<>();

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicBoolean gotMissingTransactions = new AtomicBoolean();
            Set<String> blocksReceived = Collections.synchronizedSet(new HashSet<>())
            Set<String> invBlocksReceived = Collections.synchronizedSet(new HashSet<>())

            AtomicReference<CompactBlockMsg> lastBlock = new AtomicReference<>();

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                SendHeadersMsg sendHeadersMsg = SendHeadersMsg.builder()
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, sendHeadersMsg).submit()

                SendCompactBlockMsg msg = SendCompactBlockMsg.builder()
                    .highBandwidthRelaying(true)
                    .version(1)
                    .build()
                p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()


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
                    } else {
                        println(item.getType())
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

            p2p.EVENTS.MSGS.GETHEADERS_SENT.forEach({ e ->
                println("GETHEADERS message sent")
            })

            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                println("HEADERS message received")

                println("rec: " + e.getBtcMsg())
            })

            p2p.EVENTS.MSGS.HEADERS_SENT.forEach({ e ->
                println("HEADERS message sent")
            })

            p2p.EVENTS.MSGS.SENDHEADERS.forEach({ e ->
                println("SENDHEADERS message received")
            })

            p2p.EVENTS.MSGS.SENDHEADERS_SENT.forEach({ e ->
                println("SENDHEADERS message sent")
            })

            p2p.EVENTS.MSGS.CMPCTBLOCK.forEach({ e ->
                println("CMPCTBLOCK message received")

                CompactBlockMsg msg = e.getBtcMsg().getBody()
                CompactBlockHeaderMsg header = msg.getHeader()
                String hashString = Utils.HEX.encode(header.getHash().getHashBytes())

                if (lastBlock.get() == null) {
                    lastBlock.set(msg)
                }

                blocksReceived.add(hashString)

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

            p2p.EVENTS.MSGS.GETDATA_SENT.forEach({ e ->
                println("GETDATA message sent")
            })

            p2p.EVENTS.MSGS.GETBLOCKTXN_SENT.forEach({ e ->
                println("GETBLOCKTXN message sent")
            })

            p2p.EVENTS.MSGS.GETBLOCKTXN.forEach({ e ->
                println("GETBLOCKTXN message received")
            })

            p2p.EVENTS.MSGS.BLOCKTXN.forEach({ e ->
                println("BLOCKTXN message received")
                gotMissingTransactions.set(true)
            })

            p2p.EVENTS.MSGS.BLOCKTXN_SENT.forEach({ e ->
                println("BLOCKTXN message sent")
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

    def "Testing send compact block"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(45))
                .maxPeers(OptionalInt.of(50))
                .protocolVersion(70015)
                .build()

            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .build()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            List<PeerAddress> peers = Collections.synchronizedList(new ArrayList<>());

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)

                if (e.versionMsg.version >= 70015) {
                    SendCompactBlockMsg msg = SendCompactBlockMsg.builder()
                        .highBandwidthRelaying(false)
                        .version(1)
                        .build()
                    p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                }
            })

            p2p.EVENTS.MSGS.SENDCMPCT.forEach({ e ->
                println("Send cmpct block message received")
                peers.add(e.getPeerAddress())
            })

            p2p.EVENTS.MSGS.SENDCMPCT_SENT.forEach({ e ->
                println("Send cmpct block message sent")
            })

            p2p.EVENTS.MSGS.INV.forEach({ e ->
                println("Inv message received")

                InvMessage inv = e.getBtcMsg().getBody()

                for (int i = 0; i < inv.getInvVectorList().size(); i++) {
                    InventoryVectorMsg item = inv.getInvVectorList().get(i)
                    println(item.getType())
                    if (item.getType() == InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK
                        || item.getType() == InventoryVectorMsg.VectorType.MSG_BLOCK) {
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

            p2p.EVENTS.MSGS.INV_SENT.forEach({ e ->
                println("Inv message sent")
            })

            p2p.EVENTS.MSGS.HEADERS.forEach({ e ->
                println("Headers message received")

                for (BlockHeaderMsg header : e.getBtcMsg().getBody().getBlockHeaderMsgList()) {
                    GetdataMsg msg = GetdataMsg.builder()
                        .invVectorList(List.of(
                            InventoryVectorMsg.builder()
                                .type(InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK)
                                .hashMsg(header.getHash())
                                .build()
                        ))
                        .build()

                    p2p.REQUESTS.MSGS.send(e.peerAddress, msg).submit()
                }
            })

            CompactBlockMsg compactBlockMsg = null
            p2p.EVENTS.MSGS.CMPCTBLOCK.forEach({ e ->
                println("Compact block message received")

                // request some transactions
                compactBlockMsg = e.getBtcMsg().getBody()

                InvMessage invMessage = InvMessage.builder()
                    .invVectorMsgList(List.of(
                        InventoryVectorMsg.builder()
                            .hashMsg(compactBlockMsg.getHeader().getHash())
                            .type(InventoryVectorMsg.VectorType.MSG_BLOCK)
                            .build()
                    ))
                    .build()

                for (PeerAddress peer : peers) {
                    p2p.REQUESTS.MSGS.send(peer, invMessage).submit()
                }
            })

            p2p.EVENTS.MSGS.CMPCTBLOCK_SENT.forEach({ e ->
                println("Compact block message sent")
            })

            p2p.EVENTS.MSGS.GETDATA.forEach({ e ->
                println("Getdata message received")

                CompactBlockMsg msg = CompactBlockMsg.builder()
                    .header(compactBlockMsg.getHeader())
                    .nonce(compactBlockMsg.getNonce())
                    .shortTxIds(compactBlockMsg.getShortTxIds())
                    .prefilledTransactions(new ArrayList<PrefilledTxMsg>())
                    .build()

                for (InventoryVectorMsg vector : e.getBtcMsg().getBody().getInvVectorList()) {
                    println(" - vector: " + vector.getType())

                    if (vector.getType() == InventoryVectorMsg.VectorType.MSG_CMPCT_BLOCK) {
                        for (PeerAddress peer : peers) {
                            p2p.REQUESTS.MSGS.send(peer, msg).submit()
                        }
                    }
                }
            })

            AtomicBoolean getBlockTxnReceived = new AtomicBoolean();

            p2p.EVENTS.MSGS.GETBLOCKTXN.forEach({ e ->
                println("GetBlockTxn message received")
                getBlockTxnReceived.set(true)
            })

            p2p.EVENTS.MSGS.BLOCKTXN.forEach({ e ->
                println("Blocktxn message received")
            })

        when:
            println(" > Testing get compact block message in " + config.toString() + "...")

            p2p.start()

            // wait long enough to new block be created and missing transactions are received
            while (!getBlockTxnReceived.get()) {
                Thread.sleep(1000)
            }

            p2p.stop()

        then:
            numOfHandshakes.get() > 0
            getBlockTxnReceived.get()
    }
}
