package com.nchain.jcl.net.performance

import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.events.data.TxMsgReceivedEvent
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerConfig
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import com.nchain.jcl.tools.events.EventQueueProcessor
import com.nchain.jcl.tools.thread.ThreadUtils

import io.bitcoinj.core.Sha256Hash

import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import java.util.concurrent.atomic.AtomicLong

/**
 * Performance Testing. We use a P2P Service to connect the network, and we process all the INV messages that come
 * from it. For each Tx contained in the INV,s we ask for them. Since this is just a performance testing, we do not
 * care whether we are asking for Txs already processed or not, we just need to check how many Tx/sec we can process..
 */
class TxBlasterPerformanceTest extends Specification {

    // Network we use for this test (It does NOT really matter)
    static ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()

    // Duration of the test:
    static Duration TEST_DURATION = Duration.ofSeconds(60)

    // Number of Txs processed
    static AtomicLong numTxs = new AtomicLong()

    // Time when we receive the FIRST TX:
    static Instant firstTxInstant = null

    // Executor that twill trigger a thread from where the TxBlaster will be firing Txs...
    static ExecutorService executor = ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY

    // Number of Tx/sec to Blast in general
    static int NUM_TX_SEC_GLOBAL = 6000

    // Number of TXs that each Thread is capable of launching (MAX):
    static int NUM_TX_SEC_THREAD = 300

    // The Tx that the TxBlaster will be sending (We always send the same Tx)
    static BitcoinMsg<TxMsg> TX_MSG = buildTxMsg(protocolConfig)

    /** Convenience method to generate a Dummy Tx, */
    private static BitcoinMsg<TxMsg> buildTxMsg(ProtocolConfig protocolConfig) {
        Random rand = new Random();
        // Tx Inputs:
        TxOutPointMsg txOutpointMsg = TxOutPointMsg.builder()
                .hash(HashMsg.builder().hash(Sha256Hash.ZERO_HASH.bytes).build())
                .index(5)
                .build()
        TxInputMsg txInputMsg1 = TxInputMsg.builder()
            .sequence(rand.nextLong())
            .signature_script(new byte[20])
            .pre_outpoint(txOutpointMsg)
            .build()
        TxInputMsg txInputMsg2 = TxInputMsg.builder()
                .sequence(rand.nextLong())
                .signature_script(new byte[20])
                .pre_outpoint(txOutpointMsg)
                .build()
        List<TxInputMsg> txInputMsgs = Arrays.asList(txInputMsg1, txInputMsg2)

        // Tx Outputs:
        TxOutputMsg txOutputMsg1 = TxOutputMsg.builder().txValue(50).pk_script(new byte[20]).build()
        TxOutputMsg txOutputMsg2 = TxOutputMsg.builder().txValue(50).pk_script(new byte[20]).build()
        List<TxOutputMsg> txOutputMsgs = Arrays.asList(txOutputMsg1, txOutputMsg2)

        TxMsg txMsg = TxMsg.builder()
            .lockTime(rand.nextLong())
            .version(rand.nextLong())
            .tx_in(txInputMsgs)
            .tx_out(txOutputMsgs)
            .build()

        BitcoinMsg<TxMsg> result = new BitcoinMsgBuilder<>(protocolConfig.basicConfig, txMsg).build()
        return result
    }

    /**
     * It process any incoming TX We just keep track and log it
     */
    private void processTX(TxMsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        numTxs.incrementAndGet()
        Sha256Hash txHash = Sha256Hash.wrap(event.btcMsg.body.hash.get().hashBytes)
        println(" Tx " + txHash + " from " + event.peerAddress + " [ " + numTxs.get() + " txs, " + Thread.activeCount() + " threads ]")
    }

    private void blastTxs(P2P txBlaster, PeerAddress serverAddress, int numTxsSec) {
        int millisecsToWait = 1000 / numTxsSec;
        while (true) {
            try {
                // We build a TX. We make sure it's different from others so the DeserializaerCache in the server is NOT
                // used...
                txBlaster.REQUESTS.MSGS.send(serverAddress, TX_MSG).submit()
                Thread.sleep(millisecsToWait)
            } catch (InterruptedException e) {}
        } // while..
    }

    /**
     * Main TEst.
     */
    def "Testing Tx Blaster"() {
        given:
            // We configure the MessageHandler to DISABLE the Deserializer Cache...
            MessageHandlerConfig messageConfig = protocolConfig.getMessageConfig()
            DeserializerConfig deserializerConfig = messageConfig.deserializerConfig.toBuilder().enabled(false).build()
            messageConfig = messageConfig.toBuilder().deserializerConfig(deserializerConfig).build()

            // We configure the Handshake we get notified of new Txs...
            HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                                                        .relayTxs(true)
                                                        .build()
            // We define a Range of Peers...
            ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                                                        .minPeers(OptionalInt.of(15))
                                                        .maxPeers(OptionalInt.of(18))
                                                        .build()
            // We build the P2P SERVER:
            P2P server = new P2PBuilder("server")
                            .config(protocolConfig)
                            .config(messageConfig)
                            .config(handshakeConfig)
                            .config(basicConfig)
                            .excludeHandler(DiscoveryHandler.HANDLER_ID)
                            .build()


            // We define 1 Queue where the service will be processing the incoming TX messages...
            EventQueueProcessor txProcessor = new EventQueueProcessor(ThreadUtils.EVENT_BUS_EXECUTOR_HIGH_PRIORITY);
            txProcessor.addProcessor(TxMsgReceivedEvent.class, {e -> processTX(e)})

            // we assign callbacks to some events:
            server.EVENTS.PEERS.HANDSHAKED.forEach({e -> println(e)})
            server.EVENTS.MSGS.TX.forEach({ e -> txProcessor.addEvent(e)})

            // We build the P2P SERVER:
            P2P txBlaster = new P2PBuilder("txBlaster")
                    .config(protocolConfig)
                    .config(handshakeConfig)
                    .config(basicConfig)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .build()

            txBlaster.EVENTS.PEERS.HANDSHAKED.forEach({e -> println(e)})

        when:

            // we start the Event Processing Queues...
            txProcessor.start()

            // We start the P2P Services...
            server.startServer()
            txBlaster.start()

            // We connect them together, and we trigger the blaster Thread...
            txBlaster.REQUESTS.PEERS.connect(server.peerAddress).submit()
            Thread.sleep(500)

            // Now we trigger the Threads to baslt te Tx. Depending on the desired number of TX&Sec Global, we trigger more or less
            int numTxsSecToAssing = NUM_TX_SEC_GLOBAL
            int numThreads = NUM_TX_SEC_GLOBAL / NUM_TX_SEC_THREAD

            // We craete diffferent Threads, each one will send ts at a specific Rate:
            while (numTxsSecToAssing > 0) {
                int rateForThisTread = Math.min(NUM_TX_SEC_THREAD, numTxsSecToAssing)
                println("launching TxBlasterThread with rate of " + rateForThisTread + " Txs/sec...")
                executor.submit({ -> blastTxs(txBlaster, server.peerAddress, rateForThisTread)})
                numTxsSecToAssing -= rateForThisTread;
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
