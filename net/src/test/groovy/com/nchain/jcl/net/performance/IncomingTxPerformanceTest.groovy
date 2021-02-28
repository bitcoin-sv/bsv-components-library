package com.nchain.jcl.net.performance

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.events.MsgReceivedEvent
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig
import com.nchain.jcl.net.protocol.messages.GetdataMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.InvMessage
import com.nchain.jcl.net.protocol.messages.InventoryVectorMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.messages.common.Message
import com.nchain.jcl.net.protocol.serialization.GetdataMsgSerializer
import com.nchain.jcl.net.protocol.serialization.InvMsgSerializer
import com.nchain.jcl.net.protocol.serialization.InventoryVectorMsgSerializer
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.core.Utils
import io.bitcoinj.params.MainNetParams
import org.spongycastle.util.encoders.Hex
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

class IncomingTxPerformanceTest extends Specification {

    // Duration of the test:
    Duration TEST_DURATION = Duration.ofSeconds(120)

    // Collection used to keep track of the Txs received:
    Set<Sha256Hash> TXS = ConcurrentHashMap.newKeySet()
    Lock TXS_LOCK = new ReentrantLock();

    // Number of Txs processed
    AtomicLong numTxs = new AtomicLong()

    // Tiem when we receive the FIRST TX:
    Instant firstTxInstant = null

    private void processINV(P2P p2p, MsgReceivedEvent event) {
        InvMessage invMsg = ((BitcoinMsg<InvMessage>) event.btcMsg).body
        // New Txs coming from the Network in InvMsg format...
        List<InventoryVectorMsg> newTxsInvItems = null;
        // First we lock on the TX Set and we update it with the NEW incoming Txs...
        try {
            TXS_LOCK.lock()
            // We check the Txs we do not have yet...
            newTxsInvItems = ((BitcoinMsg<InvMessage>) event.btcMsg).body.invVectorList.stream()
                                .filter({InventoryVectorMsg i -> i.type.equals(InventoryVectorMsg.VectorType.MSG_TX)})
                                .filter({InventoryVectorMsg i -> !TXS.contains(Sha256Hash.wrap(i.hashMsg.hashBytes))})
                                .collect(Collectors.toList())

            //println(event.peerAddress.toString() + " :: Incoming INV, " + newTxsInvItems.size() + " new Txs...")

            // We add them all to the Set...
            newTxsInvItems.forEach({InventoryVectorMsg h -> TXS.add(Sha256Hash.wrap(h.hashMsg.hashBytes))})
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            TXS_LOCK.unlock()
        }

        // We log the INV Items as they come WITHOUT REVERSING the Bytes...
        //newTxsInvItems.forEach({InventoryVectorMsg inv -> println(event.peerAddress.toString() +  " :: TX announced: " +  Sha256Hash.wrap(inv.hashMsg.hashBytes) + " (reversed: " + Sha256Hash.wrapReversed(inv.hashMsg.hashBytes) + ") announced.")})

        if (newTxsInvItems.size() > 0) {
            // Now we send a GetData asking for them....
            GetdataMsg getDataMsg = GetdataMsg.builder()
                    .invVectorList(newTxsInvItems)
                    .build()
            BitcoinMsg<GetdataMsg> btcGetDataMsg = new BitcoinMsgBuilder(p2p.protocolConfig.basicConfig, getDataMsg).build()
            p2p.REQUESTS.MSGS.send(event.peerAddress, btcGetDataMsg).submit()
        }
    }


    private void processTX(P2P p2p, MsgReceivedEvent event) {
        if (firstTxInstant == null) firstTxInstant = Instant.now()
        TxMsg txMsg = (TxMsg) event.getBtcMsg().body
        Sha256Hash txHash = Sha256Hash.wrap(txMsg.hash.get().hashBytes)
        numTxs.incrementAndGet()
        println(" Tx " + txHash + " from " + event.peerAddress + " (" + numTxs.get() + " txs)...")
    }

    def "Testing Incoming TX in real Mainnet"() {
        given:
            // We set up the JCL-Net Configuration, activating the incoming Tx (relayTx = TRUE)
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(MainNetParams.get())
            HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                                                        .relayTxs(true)
                                                        .build()
            ProtocolBasicConfig basicConfig = protocolConfig.getBasicConfig().toBuilder()
                                                        .minPeers(OptionalInt.of(60))
                                                        .maxPeers(OptionalInt.of(65))
                                                        .build()
            // We build the P2P Service:
            P2P p2p = new P2PBuilder("performance")
                            .config(protocolConfig)
                            .config(handshakeConfig)
                            .config(basicConfig)
                            .build()

            p2p.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({e -> println(e)})
            p2p.EVENTS.MSGS.ALL.forEach({e -> println(e)})
            //p2p.EVENTS.MSGS.ALL_SENT.forEach({e -> println(e)})
            p2p.EVENTS.MSGS.INV.forEach({e -> processINV(p2p, e)})
            p2p.EVENTS.MSGS.TX.forEach({ e -> processTX(p2p, e)})

        when:
            p2p.start()
            Thread.sleep(TEST_DURATION.toMillis())
            Instant endTime = Instant.now()
            p2p.stop()

            Duration effectiveTime = Duration.between(firstTxInstant, endTime)
            println(" >> " + TEST_DURATION.toSeconds() + " secs of Test")
            println(" >> " + effectiveTime.toSeconds() + " secs processing Txs")
            println(" >> " + numTxs.get() + " Txs processed")
            println(" >> performance: " + (numTxs.get() / effectiveTime.toSeconds()) + " txs/sec")
        then:
            true
    }
}
