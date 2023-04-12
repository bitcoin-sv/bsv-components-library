package io.bitcoinsv.bsvcl.net


import io.bitcoinsv.bsvcl.common.events.EventQueueProcessor
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils

import spock.lang.Ignore
import spock.lang.Specification
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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

    Set<io.bitcoinsv.bsvcl.net.network.PeerAddress> peersInfo = ConcurrentHashMap.newKeySet()
    Map<io.bitcoinsv.bsvcl.net.network.PeerAddress, List<io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg>> peersTxs = new ConcurrentHashMap<>();

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    /**
     * It connects the P2P Service to some Peers manually. The peers are different depending on the network. This
     * is done this way because someties we cannot waut ofr the current Node.Discovery algorithm to find them in a
     * reasonable time.
     */
    private void connectPeersManually(io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p) {

        String netId = p2p.getProtocolConfig().getId()
        if (netId.equalsIgnoreCase(io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig.id) || netId.equalsIgnoreCase(MainNetParams.get().getId())) {
            p2p.REQUESTS.PEERS.connect("95.217.93.4:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
            p2p.REQUESTS.PEERS.connect("47.91.73.203:8333").submit()
            p2p.REQUESTS.PEERS.connect("13.231.92.219:8333").submit()
            p2p.REQUESTS.PEERS.connect("52.201.98.224:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
            p2p.REQUESTS.PEERS.connect("39.100.123.126:8333").submit()
        }

        if (netId.equalsIgnoreCase(io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVStnConfig.id) || netId.equalsIgnoreCase(STNParams.get().getId())) {

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
    private void processINV(io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p, io.bitcoinsv.bsvcl.net.protocol.events.data.InvMsgReceivedEvent event) {
        List<io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg> newTxsInvItems =  event.btcMsg.body.invVectorList;
        if (MIN_PEERS == null || numPeersHandshaked >= MIN_PEERS) {

           if (newTxsInvItems.size() > 0) {

                // Now we send a GetData asking for them....
                io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg getDataMsg = io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg.builder().invVectorList(newTxsInvItems).build()
                io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg> btcGetDataMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder(p2p.protocolConfig.basicConfig, getDataMsg).build()
                p2p.REQUESTS.MSGS.send(event.getPeerAddress(), btcGetDataMsg).submit()

            }
        }
    }

    /**
     * It process any incoming TX We just keep track and log it
     */
    private void processTX(io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p, io.bitcoinsv.bsvcl.net.protocol.events.data.TxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        lastTxInstant = Instant.now()
        numTxs.incrementAndGet()
    }

    private void processRawTX(io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p, io.bitcoinsv.bsvcl.net.protocol.events.data.RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        lastTxInstant = Instant.now()
        numTxs.incrementAndGet()

    }

    /**
     * Main TEst.
     */
    @Ignore
    def "Testing Incoming TX in real Mainnet"() {
        given:
            // We set up the Network we are connecting to...
            //ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVStnConfig()
            //ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(Net.valueOf("REGTEST").params())

            // We disable the Deserialize Cache, so we force it to Deserialize all the TX, even if they are Tx that we
            // already processed...
            io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig()
            io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerConfig deserializerConfig = messageConfig.deserializerConfig.toBuilder().cacheEnabled(false).build()

            // We also activate the "RawTxs" mode
            messageConfig = messageConfig.toBuilder()
                    .rawTxsEnabled(true)
                    .deserializerConfig(deserializerConfig)
                    .build()

            // We configure the Handshake we get notified of new Txs...
            io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                                                        .relayTxs(true)
                                                        .build()

            // We raise the Timeot for PING/PONG protocols:
            io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandlerConfig pingPongConfig = protocolConfig.getPingPongConfig().toBuilder()
                .inactivityTimeout(Duration.ofSeconds(50))
                .responseTimeout(Duration.ofSeconds(120))
                .build()

            // We define a Range of Peers...
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                                                        .minPeers(OptionalInt.of(15))
                                                        .maxPeers(OptionalInt.of(30))
                                                        .build()
            // We build the P2P Service:
            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("performance")
                            .config(protocolConfig)
                            .config(pingPongConfig)
                            .config(messageConfig)
                            .config(handshakeConfig)
                            .config(basicConfig)
                            //.excludeHandler(PingPongHandler.HANDLER_ID)
                           // .excludeHandler(DiscoveryHandler.HANDLER_ID)
                            .build()


            // We define 2 Queues where well be processing the incoming INV and TX messages...
            EventQueueProcessor invProcessor = new EventQueueProcessor("inv-Queue", ThreadUtils.getFixedThreadExecutorService("inv-Queue", 100));
            EventQueueProcessor txProcessor = new EventQueueProcessor("tx-Queue",ThreadUtils.getFixedThreadExecutorService("inv-Queue", 100));
            invProcessor.addProcessor(io.bitcoinsv.bsvcl.net.protocol.events.data.InvMsgReceivedEvent.class, { e -> processINV(p2p, e)})
            txProcessor.addProcessor(io.bitcoinsv.bsvcl.net.protocol.events.data.TxMsgReceivedEvent.class, { e -> processTX(p2p, e)})
            txProcessor.addProcessor(io.bitcoinsv.bsvcl.net.protocol.events.data.RawTxMsgReceivedEvent.class, { e -> processRawTX(p2p, e)})

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
            //p2p.start()
            p2p.startServer()

            // We connect to some nodes manually, in order to improve Node Discovery:
            connectPeersManually(p2p);

            // We start the logging Thread:
            executor.scheduleAtFixedRate({-> this.log()}, 0, 1, TimeUnit.SECONDS)

            // we let it running for some time, and then we measure times in order to calculate statistics:
            Thread.sleep(TEST_DURATION.toMillis())

            p2p.stop()
            txProcessor.stop()
            invProcessor.stop()

            executor.shutdownNow()

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


    void log() {
        StringBuffer logLine = new StringBuffer()
        // Performance log:
        logLine.append(":: Performance : " +  numPeersHandshaked + " peers, " + numTxs.get() + " Txs received.");
        // Threads log:
        logLine.append("\n");
        logLine.append(":: Threads     : " + ThreadUtils.getThreadsInfo())
        // Memory log:
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMem = Runtime.getRuntime().maxMemory() - usedMem;
        String memorySummary = String.format("totalMem: %s, maxMem: %s, freeMem: %s, usedMem: %s, availableMem: %s",
                Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().maxMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), false),
                Utils.humanReadableByteCount(usedMem, false),
                Utils.humanReadableByteCount(availableMem, false))

        logLine.append("\n");
        logLine.append(":: Memory      : ").append(memorySummary);
        println(logLine.toString());

    }
}
