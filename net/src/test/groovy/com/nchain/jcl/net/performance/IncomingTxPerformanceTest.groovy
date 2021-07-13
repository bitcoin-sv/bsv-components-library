package com.nchain.jcl.net.performance

import com.google.common.util.concurrent.RateLimiter
import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVStnConfig
import com.nchain.jcl.net.protocol.events.data.InvMsgReceivedEvent
import com.nchain.jcl.net.protocol.events.data.RawTxMsgReceivedEvent
import com.nchain.jcl.net.protocol.events.data.TxMsgReceivedEvent
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandlerConfig
import com.nchain.jcl.net.protocol.messages.GetdataMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerConfig
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import com.nchain.jcl.tools.events.EventQueueProcessor
import com.nchain.jcl.tools.thread.ThreadUtils
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.STNParams
import spock.lang.Ignore
import spock.lang.Specification
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors


/**
 * Performance Testing. We use a P2P Service to connect the network, and we process all the INV messages that come
 * from it. For each Tx contained in the INV,s we ask for them. Sinc ethis is just a performance testing, we do not
 * care whether we are asking for Txs already processed or not, we just need to check how many Tx/sec we can process..
 */
class IncomingTxPerformanceTest extends Specification {

    // Duration of the test:
    Duration TEST_DURATION = Duration.ofSeconds(240)

    // Min number of Peers before we start asking for Txs (NULL for no limit)
    Integer MIN_PEERS = null

    // Number of Txs processed
    AtomicLong numTxs = new AtomicLong()
    AtomicInteger numPeersHandshaked = new AtomicInteger()

    // Time when we receive the FIRST and LAST TXs:
    Instant firstTxInstant = null
    Instant lastTxInstant = null;

    Set<PeerAddress> peersInfo = ConcurrentHashMap.newKeySet()
    Map<PeerAddress, List<InventoryVectorMsg>> peersTxs = new ConcurrentHashMap<>();

    /**
     * It connects the P2P Service to some Peers manually. The peers are different depending on the network. This
     * is done this way because someties we cannot waut ofr the current Node.Discovery algorithm to find them in a
     * reasonable time.
     */
    private void connectPeersManually(P2P p2p) {

        String netId = p2p.getProtocolConfig().getId()
        if (netId.equalsIgnoreCase(ProtocolBSVMainConfig.id) || netId.equalsIgnoreCase(MainNetParams.get().getId())) {
            p2p.REQUESTS.PEERS.connect("95.217.93.4:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
            p2p.REQUESTS.PEERS.connect("47.91.73.203:8333").submit()
            p2p.REQUESTS.PEERS.connect("13.231.92.219:8333").submit()
            p2p.REQUESTS.PEERS.connect("52.201.98.224:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
        }

        if (netId.equalsIgnoreCase(ProtocolBSVStnConfig.id) || netId.equalsIgnoreCase(STNParams.get().getId())) {

            // Brad:
            p2p.REQUESTS.PEERS.connect("209.97.128.49:9333").submit()
            p2p.REQUESTS.PEERS.connect("188.166.44.242:9333").submit()
            p2p.REQUESTS.PEERS.connect("165.22.58.146:9333").submit()
            p2p.REQUESTS.PEERS.connect("206.189.42.110:9333").submit()
            p2p.REQUESTS.PEERS.connect("165.22.59.150:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.171.166:9333").submit()
            p2p.REQUESTS.PEERS.connect("95.217.38.94:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.113.92:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.118.183:9333").submit()
            p2p.REQUESTS.PEERS.connect("46.4.76.249:9333").submit()
            p2p.REQUESTS.PEERS.connect("95.217.121.173:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.234.249:9333").submit()
            p2p.REQUESTS.PEERS.connect("95.217.108.109:9333").submit()

            // Esthon:
            p2p.REQUESTS.PEERS.connect("104.154.79.59:9333").submit()

            p2p.REQUESTS.PEERS.connect("35.184.152.150:9333").submit()
            p2p.REQUESTS.PEERS.connect("35.188.22.213:9333").submit()
            p2p.REQUESTS.PEERS.connect("35.224.150.17:9333").submit()
            p2p.REQUESTS.PEERS.connect("104.197.96.163:9333").submit()
            p2p.REQUESTS.PEERS.connect("34.68.205.136:9333").submit()
            p2p.REQUESTS.PEERS.connect("34.70.95.165:9333").submit()
            p2p.REQUESTS.PEERS.connect("34.70.152.148:9333").submit()
            p2p.REQUESTS.PEERS.connect("104.154.79.59:9333").submit()
            p2p.REQUESTS.PEERS.connect("35.232.247.207:9333").submit()


            // whatsOnChain:
            p2p.REQUESTS.PEERS.connect("37.122.249.164:9333").submit()
            p2p.REQUESTS.PEERS.connect("95.217.121.173:9333").submit()

            p2p.REQUESTS.PEERS.connect("165.22.58.146:9333").submit()
            p2p.REQUESTS.PEERS.connect("46.4.76.249:9333").submit()
            p2p.REQUESTS.PEERS.connect("134.122.102.58:9333").submit()
            p2p.REQUESTS.PEERS.connect("178.128.169.224:9333").submit()
            p2p.REQUESTS.PEERS.connect("206.189.42.110:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.171.166:9333").submit()


            p2p.REQUESTS.PEERS.connect("178.62.11.170:9333").submit()
            p2p.REQUESTS.PEERS.connect("34.70.152.148:9333").submit()
            p2p.REQUESTS.PEERS.connect("95.217.121.173:9333").submit()
            p2p.REQUESTS.PEERS.connect("116.202.118.183:9333").submit()
            p2p.REQUESTS.PEERS.connect("139.59.78.14:9333").submit()

            p2p.REQUESTS.PEERS.connect("37.122.249.164:9333").submit()

            p2p.REQUESTS.PEERS.connect("165.22.127.22:9333").submit()
            p2p.REQUESTS.PEERS.connect("78.110.160.26:9333").submit()
            p2p.REQUESTS.PEERS.connect("209.97.181.106:9333").submit()
            p2p.REQUESTS.PEERS.connect("64.227.40.244:9333").submit()
            p2p.REQUESTS.PEERS.connect("35.184.152.150:9333").submit()
            p2p.REQUESTS.PEERS.connect("212.89.6.129:9333").submit()


        }
    }

    /**
     * It process each incoming INV: It just sends a GET_DATA message back to the Peer, aasking for ALL the Txs
     * announcement in the INV.
     */
    private void processINV(P2P p2p, InvMsgReceivedEvent event) {
        List<InventoryVectorMsg> newTxsInvItems =  event.btcMsg.body.invVectorList;

        StringBuffer logLine = new StringBuffer();
        logLine.append(":: INV (" + newTxsInvItems.size() + " items) ")
        logLine.append("[").append(numTxs.get()).append(" txs] ");
        logLine.append(numPeersHandshaked + " peers.")

        if (MIN_PEERS == null || numPeersHandshaked >= MIN_PEERS) {

            logLine.append("\n");
            logLine.append(":: Threads : " + ThreadUtils.getThreadsInfo())

            long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long availableMem = Runtime.getRuntime().maxMemory() - usedMem;
            String memorySummary = String.format("totalMem: %s, maxMem: %s, freeMem: %s, usedMem: %s, availableMem: %s",
                    Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), false),
                    Utils.humanReadableByteCount(Runtime.getRuntime().maxMemory(), false),
                    Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), false),
                    Utils.humanReadableByteCount(usedMem, false),
                    Utils.humanReadableByteCount(availableMem, false))

            logLine.append("\n");
            logLine.append(":: ").append(memorySummary);

            if (newTxsInvItems.size() > 0) {

                // Now we send a GetData asking for them....
                GetdataMsg getDataMsg = GetdataMsg.builder().invVectorList(newTxsInvItems).build()
                BitcoinMsg<GetdataMsg> btcGetDataMsg = new BitcoinMsgBuilder(p2p.protocolConfig.basicConfig, getDataMsg).build()
                p2p.REQUESTS.MSGS.send(event.getPeerAddress(), btcGetDataMsg).submit()

            }
        }

        println(logLine.toString())
    }

    /**
     * It process any incoming TX We just keep track and log it
     */
    private void processTX(P2P p2p, TxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        lastTxInstant = Instant.now()
        numTxs.incrementAndGet()
        Sha256Hash txHash = Sha256Hash.wrap(event.btcMsg.body.hash.get().hashBytes)
        println(" Tx " + txHash + " from " + event.peerAddress + " [ " + numTxs.get() + " txs, " + Thread.activeCount() + " threads, " + numPeersHandshaked.get() + " peers handshaked ]")
    }

    private void processRawTX(P2P p2p, RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        lastTxInstant = Instant.now()
        numTxs.incrementAndGet()
        //Sha256Hash txHash = event.btcMsg.body.hash
        //println(" Tx " + txHash + " from " + event.peerAddress + " [ " + numTxs.get() + " txs, " + Thread.activeCount() + " threads, " + numPeersHandshaked.get() + " peers handshaked ]")
    }

    /**
     * Main TEst.
     */
    @Ignore
    def "Testing Incoming TX in real Mainnet"() {
        given:
            // We set up the Network we are connecting to...
            //ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()
            ProtocolConfig protocolConfig = new ProtocolBSVStnConfig()

            // We disable the Deserialize Cache, so we force it to Deserialize all the TX, even if they are Tx that we
            // already processed...
            MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig()
            DeserializerConfig deserializerConfig = messageConfig.deserializerConfig.toBuilder().cacheEnabled(false).build()

            // We also activate the "RawTxs" mode
            messageConfig = messageConfig.toBuilder()
                    .rawTxsEnabled(true)
                    .deserializerConfig(deserializerConfig)
                    .build()

            // We configure the Handshake we get notified of new Txs...
            HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                                                        .relayTxs(true)
                                                        .build()

            // We raise the Timeot for PING/PONG protocols:
            PingPongHandlerConfig pingPongConfig = protocolConfig.getPingPongConfig().toBuilder()
                .inactivityTimeout(Duration.ofSeconds(50))
                .responseTimeout(Duration.ofSeconds(120))
                .build()

            // We define a Range of Peers...
            ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                                                        .minPeers(OptionalInt.of(15))
                                                        .maxPeers(OptionalInt.of(18))
                                                        .build()
            // We build the P2P Service:
            P2P p2p = new P2PBuilder("performance")
                            .config(protocolConfig)
                            .config(pingPongConfig)
                            .config(messageConfig)
                            .config(handshakeConfig)
                            .config(basicConfig)
                            .excludeHandler(DiscoveryHandler.HANDLER_ID)
                            .build()


            // We define 2 Queues where well be processing the incoming INV and TX messages...
            EventQueueProcessor invProcessor = new EventQueueProcessor("inv-Queue", ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY);
            EventQueueProcessor txProcessor = new EventQueueProcessor("tx-Queue",ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY);
            invProcessor.addProcessor(InvMsgReceivedEvent.class, {e -> processINV(p2p, e)})
            txProcessor.addProcessor(TxMsgReceivedEvent.class, {e -> processTX(p2p, e)})
            txProcessor.addProcessor(RawTxMsgReceivedEvent.class, { e -> processRawTX(p2p, e)})

            // we assign callbacks to some events:

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({e ->
                println(e)
                numPeersHandshaked.incrementAndGet()
                peersInfo.add(e.peerAddress)
            })
            p2p.EVENTS.PEERS.HANDSHAKED_DISCONNECTED.forEach({e ->
                numPeersHandshaked.decrementAndGet();
                peersInfo.remove(e.peerAddress)
            })
            p2p.EVENTS.PEERS.CONNECTED.forEach({e -> println(e)})
            p2p.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({e -> println(e)})
            p2p.EVENTS.MSGS.INV.forEach({e ->
                //println("ADDING INV TO PROCESSOR")
                invProcessor.addEvent(e)
            })
            //p2p.EVENTS.MSGS.TX.forEach({ e -> txProcessor.addEvent(e)})
            p2p.EVENTS.MSGS.TX_RAW.forEach({ e ->
                //println("ADDING TX TO PROCESSOR")
                txProcessor.addEvent(e)
            })

        when:

            // we start the Event Processing Queues...
            txProcessor.start()
            invProcessor.start()

            // We start the P2P Service...
            p2p.start()

            // We connect to some nodes manually, in order to improve Node Discovery:
            connectPeersManually(p2p);

            // we let it running for some time, and then we measure times in order to calculate statistics:
            Thread.sleep(TEST_DURATION.toMillis())

            p2p.stop()
            txProcessor.stop()
            invProcessor.stop()

            // We print the Summary:
            Duration effectiveTime = Duration.between(firstTxInstant, lastTxInstant)
            println(" >> " + TEST_DURATION.toSeconds() + " secs of Test")
            println(" >> " + effectiveTime.toSeconds() + " secs processing Txs")
            println(" >> " + numTxs.get() + " Txs processed")
            println(" >> performance: " + (numTxs.get() / effectiveTime.toSeconds()) + " txs/sec")
            println(p2p.getEventBus().getStatus())
        then:
            true
    }
}
