package io.bitcoinsv.bsvcl.net

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import io.bitcoinsv.bsvcl.common.events.EventQueueProcessor
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance Testing. We use a P2P Service to connect the network, and we process all the INV messages that come
 * from it. For each Tx contained in the INV,s we ask for them. Since this is just a performance testing, we do not
 * care whether we are asking for Txs already processed or not, we just need to check how many Tx/sec we can process..
 */
class TxBlasterPerformanceTest extends Specification {

    // Network we use for this test (It does NOT really matter)
    static io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig()

    // Duration of the test:
    static Duration TEST_DURATION = Duration.ofSeconds(30)

    // Number of Txs processed
    static AtomicLong numTxs = new AtomicLong()

    // Time when we receive the FIRST TX:
    static Instant firstTxInstant = null

    // Executor that twill trigger a thread from where the TxBlaster will be firing Txs...
    static ExecutorService executor =  ThreadUtils.getFixedThreadExecutorService("txBlaster-Performance", 100);

    // Number of Tx/sec to Blast in general
    static int NUM_TX_SEC = 4000

    // Number of TXs sent in each batch
    static int NUM_TX_BATCH = 100

    // Milisecs it takes to send one Batch of Txs (estimate)
    static int MILLISECS_PROCESSING_BATCH = 300;

    // Maximun Rate (Txs/Sec) sent by each Thread:
    static int MAX_TX_SEC_THREAD = 200


    /** Convenience method to generate a Dummy Tx, */
    private static io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg> buildTxMsg(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig) {
        Random rand = new Random();
        // Tx Inputs:
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg txOutpointMsg = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutPointMsg.builder()
                .hash(io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(Sha256Hash.ZERO_HASH.bytes).build())
                .index(5)
                .build()
        io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg txInputMsg1 = io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg.builder()
                .sequence(rand.nextLong())
                .signature_script(new byte[20])
                .pre_outpoint(txOutpointMsg)
                .build()
        io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg txInputMsg2 = io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg.builder()
                .sequence(rand.nextLong())
                .signature_script(new byte[20])
                .pre_outpoint(txOutpointMsg)
                .build()
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxInputMsg> txInputMsgs = Arrays.asList(txInputMsg1, txInputMsg2)

        // Tx Outputs:
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMsg1 = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(50).pk_script(new byte[20]).build()
        io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg txOutputMsg2 = io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg.builder().txValue(50).pk_script(new byte[20]).build()
        List<io.bitcoinsv.bsvcl.net.protocol.messages.TxOutputMsg> txOutputMsgs = Arrays.asList(txOutputMsg1, txOutputMsg2)

        io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg txMsg = io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg.builder()
                .lockTime(rand.nextLong())
                .version(rand.nextLong())
                .tx_in(txInputMsgs)
                .tx_out(txOutputMsgs)
                .build()

        io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.TxMsg> result = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(protocolConfig.basicConfig, txMsg).build()
        return result
    }

    private static io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg> buildRawTxMsg(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig) {
        // Body Message in Hex Format:
        String REF_MSG ="0500000001bad09aa61d4fff3bba3fb8537dedd6db898996303ac2107060e430c16bb2208f010000000c6a0a00000000000000000000050000000105000000000000000c6a0a0000000000000000000005000000"

        // We get the content in byte format and
        byte[] content = Utils.HEX.decode(REF_MSG)
        HashFunction hashFunction = Hashing.sha256()

        Sha256Hash txHash =  Sha256Hash.wrapReversed(hashFunction.hashBytes(hashFunction.hashBytes(content).asBytes()).asBytes());
        io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg rawTsmg = new io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg(content, txHash)

        io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg> result = new io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder<>(protocolConfig.basicConfig, rawTsmg).build()
        return result
    }

    /**
     * It process any incoming TX We just keep track and log it
     */
    private void processTX(io.bitcoinsv.bsvcl.net.protocol.events.data.TxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        numTxs.incrementAndGet()
        Sha256Hash txHash = Sha256Hash.wrap(event.btcMsg.body.hash.get().hashBytes)
        println(" Tx " + txHash + " from " + event.peerAddress + " [ " + numTxs.get() + " txs, " + Thread.activeCount() + " threads ]")
    }

    private void processRawTX(io.bitcoinsv.bsvcl.net.protocol.events.data.RawTxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        numTxs.incrementAndGet()
        Sha256Hash txHash = event.btcMsg.body.getHash()

        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long availableMem = Runtime.getRuntime().maxMemory() - usedMem;
        String memorySummary = String.format("totalMem: %s, maxMem: %s, freeMem: %s, usedMem: %s, availableMem: %s",
                Utils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().maxMemory(), false),
                Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), false),
                Utils.humanReadableByteCount(usedMem, false),
                Utils.humanReadableByteCount(availableMem, false))

        println(" Tx " + txHash + " [ " + numTxs.get() + " txs, " + Thread.activeCount() + " threads ], memory: " + memorySummary)
    }

    private void blastTxs(String threadId, io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P txBlaster, io.bitcoinsv.bsvcl.net.network.PeerAddress serverAddress, List<io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<?>> msgs, int txSecRate) {
        int numBatchsToSend = txSecRate / NUM_TX_BATCH
        int waitingTime = (1000 / numBatchsToSend)
        int waitingTimelusBuffer = waitingTime - MILLISECS_PROCESSING_BATCH
        println(">> " + threadId + " :: " + txSecRate + " txs/sec, " + numBatchsToSend + " batches/sec, " + waitingTimelusBuffer + " waiting time (millisecs)")

        while (true) {
            try {
                // We build a TX. We make sure it's different from others so the DeserializaerCache in the server is NOT
                // used...
                //txBlaster.REQUESTS.MSGS.send(serverAddress, TX_MSG).submit()
                for (int i = 0; i < numBatchsToSend; i++) {
                    txBlaster.REQUESTS.MSGS.send(serverAddress, msgs).submit()
                    Thread.sleep(waitingTimelusBuffer)
                }
            } catch (InterruptedException e) {}
        } // while..
    }

    /**
     * Main TEst.
     */
    @Ignore
    def "Testing Tx Blaster"() {
        given:
            // We configure the MessageHandler to DISABLE the Deserializer Cache...
            io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig()
            io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerConfig deserializerConfig = messageConfig.deserializerConfig.toBuilder().cacheEnabled(false).build()
            messageConfig = messageConfig.toBuilder()
                    .deserializerConfig(deserializerConfig)
                    .rawTxsEnabled(true)
                    .build()

            // We configure the Handshake we get notified of new Txs...
            io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                                                        .relayTxs(true)
                                                        .build()
            // We define a Range of Peers...
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                                                        .minPeers(OptionalInt.of(15))
                                                        .maxPeers(OptionalInt.of(18))
                                                        .build()
            // We build the P2P SERVER:
            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P server = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("server")
                            .config(protocolConfig)
                            .config(messageConfig)
                            .config(handshakeConfig)
                            .config(basicConfig)
                            .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler.HANDLER_ID)
                            .build()


            // We define 1 Queue where the service will be processing the incoming TX messages...
            EventQueueProcessor txProcessor = new EventQueueProcessor("Test", ThreadUtils.getFixedThreadExecutorService("tx-Queue", 100));
            txProcessor.addProcessor(io.bitcoinsv.bsvcl.net.protocol.events.data.TxMsgReceivedEvent.class, { e -> processTX(e)})
            txProcessor.addProcessor(io.bitcoinsv.bsvcl.net.protocol.events.data.RawTxMsgReceivedEvent.class, { e -> processRawTX(e)})

            // we assign callbacks to some events:
            server.EVENTS.PEERS.HANDSHAKED.forEach({e -> println(e)})
            server.EVENTS.MSGS.TX.forEach({ e -> txProcessor.addEvent(e)})
            server.EVENTS.MSGS.TX_RAW.forEach({ e -> txProcessor.addEvent(e)})

            // We build the P2P SERVER:
            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P txBlaster = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("txBlaster")
                    .config(protocolConfig)
                    .config(handshakeConfig)
                    .config(basicConfig)
                    .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler.HANDLER_ID)
                    .build()

            txBlaster.EVENTS.PEERS.HANDSHAKED.forEach({e -> println(e)})

        when:

            // We pre-build a Vector of Txs...
            List<io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg>> TXS = new ArrayList<>()
            for (int i = 0; i < NUM_TX_BATCH; i++) TXS.add(buildRawTxMsg(protocolConfig))

            // we start the Event Processing Queues...
            txProcessor.start()

            // We start the P2P Services...
            server.startServer()
            txBlaster.start()

            // We connect them together, and we trigger the blaster Thread...
            txBlaster.REQUESTS.PEERS.connect(server.peerAddress).submit()
            Thread.sleep(500)

            //txBlaster.REQUESTS.MSGS.send(server.peerAddress, TX_RAW_MSG).submit();
            int numTxsSecLeft = NUM_TX_SEC
            int currentThread = 0;
            // We create different Threads, each one will send ts at a specific Rate:
            while (numTxsSecLeft > 0) {
                int threadTxSecRate = Math.min(numTxsSecLeft, MAX_TX_SEC_THREAD)
                String threadName = " thread-blaster-" + currentThread
                println("launching TxBlasterThread (" + threadTxSecRate + " txs/sec) ...")
                executor.submit({ -> blastTxs(threadName, txBlaster, server.peerAddress, TXS, threadTxSecRate)})
                numTxsSecLeft -= threadTxSecRate
                currentThread++;
            }

            // we let it running for some time, and then we measure times in order to calculate statistics:
            Thread.sleep(TEST_DURATION.toMillis())
            Instant endTime = Instant.now()

            server.stop()
            txProcessor.stop()
            executor.shutdownNow()

            // We print the Summary:
            Duration effectiveTime = Duration.between(firstTxInstant, endTime)
            println(" >> " + TEST_DURATION.toSeconds() + " secs of Test")
            println(" >> " + effectiveTime.toSeconds() + " secs processing Txs")
            println(" >> " + numTxs.get() + " Txs processed")
            println(" >> performance: " + (numTxs.get() / effectiveTime.toSeconds()) + " txs/sec")
            println(server.getEventBus().getStatus())
        then:
            true
    }
}
