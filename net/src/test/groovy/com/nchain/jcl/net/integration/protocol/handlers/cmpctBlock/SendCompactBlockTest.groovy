package com.nchain.jcl.net.integration.protocol.handlers.cmpctBlock

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.params.MainNetParams
import io.bitcoinj.params.Net
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * An integration Test for Downloading Blocks.
 */
class SendCompactBlockTest extends Specification {

    def "Testing SendCmpctBlockMsg"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(15))
                .maxPeers(OptionalInt.of(20))
                .protocolVersion(70015)
                .build()

            P2P p2p = new P2PBuilder("testing")
                .config(config)
                .config(basicConfig)
                .build()

            AtomicInteger numOfHandshakes = new AtomicInteger()
            AtomicInteger numOfSendCmpctBlockMsgReceived = new AtomicInteger()

            p2p.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numOfHandshakes.set(numOfHandshakes.get() + 1)
                println(" - Peer connected: " + e.peerAddress + " - " + e.versionMsg.version)
            })

            p2p.EVENTS.MSGS.SENDCMPCT.forEach({ e ->
                numOfSendCmpctBlockMsgReceived.set(numOfSendCmpctBlockMsgReceived.get() + 1)
                println("Send cmpct block message received")
            })

        when:
            println(" > Testing sendcmpct message in " + config.toString() + "...")

            p2p.start()

            Thread.sleep(30 * 1000)

            p2p.stop();

        then:
            numOfSendCmpctBlockMsgReceived.get() > 0
            numOfHandshakes.get() > 0
    }

}
