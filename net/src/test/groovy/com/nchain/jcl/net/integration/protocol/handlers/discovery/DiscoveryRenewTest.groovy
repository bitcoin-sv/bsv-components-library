package com.nchain.jcl.net.integration.protocol.handlers.discovery

import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.params.MainNetParams
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Test Class for the Discovery Handler Renew Process.
 *
 * The Discovery Handler has 2 processes responsible for renewing the POOL of Addresses:
 * - POOL Renew Process: The "normal" process. On a frequency basis, this process selects some of the Peers we
 *   already have in our Pool, and will ask them for more addresses.
 *
 * - HANDSHAKE Renew Process: This process also runs on a frequency basis. It also selects Peers from our POOL to ask
 *   them for new Addresses, but this time it only focus on those Peers that have ever been handshaked.
 *
 * INTERNET CONNECTION IS NEEDED FOR THIS TEST.
 */
class DiscoveryRenewTest extends Specification {

    /**
     * We check that the discovery Handler, after some time, automatically triggers the process to renew the
     * pool of addresses, by selecting some of the Peers in the Pool and asking them for new Addresses, following
     * the Node-discovery protocol (Sending GET_ADDR and receiving ADDR messages).
     */
    def "Testing POOL Renew Job"() {
        given:
            // Time to wait until the renewing process is triggered:
            final Duration triggerTime = Duration.ofMillis(5000)
            // We limit the number of Peer we want to connect to:
            final int MIN_PEERS = 3
            final int MAX_PEERS = 6

            // We set up the configuration
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams()).toBuilder()
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            // We set up the frequency for the "pool" renewing Job and  disable the "handshake" renewing job:
            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .ADDRFrequency(Optional.of(triggerTime))
                .recoveryHandshakeFrequency(Optional.empty())
                .build()
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .serverPort(0) // Random Port
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We keep track of the GET_ADDR and ADDR messages exchanged:
            AtomicInteger numGET_ADDR = new AtomicInteger()
            AtomicInteger numADDR = new AtomicInteger()
            server.EVENTS.PEERS.HANDSHAKED.forEach({e -> println(e)})
            server.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({e -> println(e)})
            server.EVENTS.MSGS.ADDR.forEach({ e -> numADDR.incrementAndGet()})
            server.EVENTS.MSGS.GETADDR_SENT.forEach({ e ->
                numGET_ADDR.incrementAndGet()
                println(e)
            })

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()
            Thread.sleep(25000) // raise this number if the DNS are poor and takes long to find peers

            // At this point, the service should have reached the MAX number of Peers handshaked, so the number of
            // connections should be stable (== MAX_PEERS). No GET_ADDR or ADDR messages should be being exchanged
            // at this moment.

            // IMPORTANT: We reset the counters of the GET_ADDR and ADDR messages exhanged:
            numGET_ADDR.set(0)
            numADDR.set(0)

            // Now we wait some time, to guarantee that the Renewing process is triggered:
            Thread.sleep(10000)
            // At this moment the Renewing process must have been triggered, so some GET_ADDR should have been
            // sent to some Peers, asking for new addresses...
            server.stop()

        then:
            // We check that GET_ADDR messages have sent out (after we reset the counter) , so that means that
            // the renewing process has been triggered...
            numGET_ADDR.get() > 0
    }

    /**
     * We check that the discovery Handler, after some time, automatically triggers the process to renew the
     * pool of addresses by trying to RECOVER Peers that had been Handshaked but are NOT anymore.
     */
    // NOTE: This test is disabled, since this scenario is hard to test. This test connects to some Peers, and after
    // it reaches the MAX peers, it wait a bit and then it disconnects from one of them. In theory, after waiting a
    // bit longer after that, the renewing process will be trigger and the service will try to re-connect to that
    // peer we disconnected from earlier. Problem is: the service is designed in a way that it will always try to keep
    // the number of connection stable, so, even if we disconnect from a Peer, the system will use the "Pool" of
    // addresses to look for and connect to new Peers again. And chances are that the Peer we just disconnect from is
    // part of that pool, so the system will re-connect automatically to that Peer right after we disconnect from it,
    // making this test useless.
    @Ignore
    def "Testing HANDSHAKE Renew Job"() {
        given:
            // Configuration of the Renewing process:
            // Frequency of the Job:
            final Duration jobFrequency = Duration.ofMillis(4000)
            // Tie to wait since the Peer has handshaked for the last time
            final Duration waitingtime = Duration.ofSeconds(1)

            // We limit the number of Peer we want to connect to:
            final int MIN_PEERS = 3
            final int MAX_PEERS = 6

            // We set up the configuration
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())

            // We set up the "handshake" renewing process and we disable the "pool" renewing process:
            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                    .recoveryHandshakeFrequency(Optional.of(jobFrequency))
                    .recoveryHandshakeThreshold(Optional.of(waitingtime))
                    .build()

            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            // We keep track of the Peer Handshaked:
            List<PeerAddress> peersHandshaked = new ArrayList<>()

            // We keep track of the PEERs we send GET_ADDR msgs to:
            List<PeerAddress> peersGetAddrSent = new ArrayList<>()


            server.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                peersHandshaked.add(e.peerAddress)
                println("Peer: " + e.getPeerAddress() + " : services: " + e.getVersionMsg().services)
            })
            server.EVENTS.MSGS.GETADDR_SENT.forEach({ e -> peersGetAddrSent.add(e.peerAddress)})

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()
            Thread.sleep(2000)

            // At this point, the service should have reached the MAX number of Peers handshaked, so the number of
            // connections should be stable (== MAX_PEERS). No GET_ADDR or ADDR messages should be being exchanged
            // at this moment.
            // IMPORTANT: We reset the list of Peers,
            peersGetAddrSent.clear()

            // Now, we are disconnecting from a Handshaked Peer.
            println(" >> DISCONNECTING FROM " + peersHandshaked.get(0) + "...")
            server.REQUESTS.PEERS.disconnect(peersHandshaked.get(0)).submit()

            // And we wait so the Renewing process is triggered.
            Thread.sleep(6000)

            // At this moment the Renewing process must have been triggered, so some GET_ADDR should have been
            // sent to some Peers, asking for new addresses...
            println(" >> STOPPING...")
            server.stop()

        then:
            // We check that GET_ADDR messages have sent out (after we reset the counter) , so that means that
            // the renewing process has been triggered...
            peersGetAddrSent.size() > 0
            // We also check that the GET_ADDR sent by the "handshake" renewing process is addressed to the Peer that
            // was handshaked but we disconnected from...
            peersGetAddrSent.get(0).equals(peersHandshaked.get(0))
    }
}
