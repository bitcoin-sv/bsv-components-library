package com.nchain.jcl.protocol.wiki;

import com.nchain.jcl.protocol.config.ProtocolConfig;
import com.nchain.jcl.protocol.config.ProtocolServices;
import com.nchain.jcl.protocol.config.ProtocolVersion;
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig;
import com.nchain.jcl.protocol.events.PeerHandshakedEvent;
import com.nchain.jcl.protocol.handlers.handshake.HandshakeHandlerConfig;
import com.nchain.jcl.protocol.messages.InvMessage;
import com.nchain.jcl.protocol.messages.InventoryVectorMsg;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.wrapper.P2P;
import com.nchain.jcl.protocol.wrapper.P2PBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-21 14:31
 *
 * Pieces of code used as examples for the "wiki" of the JCL project.
 */
public class WikiExamples {

    @Ignore
    public void example1() {
        try {
            P2P p2p = new P2PBuilder("testing").build();
            p2p.EVENTS.PEERS.CONNECTED.forEach(System.out::println);
            p2p.EVENTS.PEERS.DISCONNECTED.forEach(System.out::println);
            p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
            p2p.EVENTS.MSGS.ALL.forEach(System.out::println);
            p2p.start();
            // We will be notified for 10 seconds...
            Thread.sleep(10_000);
            p2p.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    @Ignore

    public void exampleConfig() {
        try {
            // BSV Main: ProtocolConfig config = new ProtocolBSVMainConfig();
            ProtocolConfig config = new ProtocolBSVMainConfig().toBuilder()
                    .magicPackage(0xe8f3e1e3L)
                    .port(8333)
                    .protocolVersion(ProtocolVersion.CURRENT.getBitcoinProtocolVersion())
                    .services(ProtocolServices.NODE_BLOOM.getProtocolServices())
                    .build();
            P2P p2p = new P2PBuilder("testing")
                    .config(config)
                    .minPeers(10)
                    .maxPeers(15)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
    @Ignore
    public void exampleEventHandling() {
        try {
            P2P p2p = new P2PBuilder("testing").build();
            p2p.EVENTS.PEERS.HANDSHAKED.forEach(this::onPeerHandshaked);
            p2p.start();
            Thread.sleep(10_000);
            p2p.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void onPeerHandshaked(PeerHandshakedEvent event) {
        if (event.getVersionMsg().getVersion() < 70013)
            System.out.println("Version too low!!!");
    }

    @Ignore
    public void exampleStatus() {
        try {
            P2P p2p = new P2PBuilder("testing")
                    .publishStates(Duration.ofSeconds(5))
                    .build();
            p2p.EVENTS.PEERS.CONNECTED.forEach(System.out::println);
            p2p.EVENTS.PEERS.DISCONNECTED.forEach(System.out::println);
            p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
            p2p.EVENTS.MSGS.ALL.forEach(System.out::println);
            p2p.EVENTS.STATE.ALL.forEach(System.out::println);
            p2p.start();
            Thread.sleep(10_000);
            p2p.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Ignore
    public void testPerformance() {
        try {

            ProtocolConfig config = new ProtocolBSVMainConfig();
            HandshakeHandlerConfig handshakeConfig = config.getHandshakeConfig().toBuilder()
                    .relayTxs(false)
                    .build();
            P2P p2p = new P2PBuilder("testing")
                    .publishStates(Duration.ofSeconds(1))
                    .config(config)
                    .config(handshakeConfig)
                    .minPeers(40)
                    .maxPeers(45)
                    .build();
            //p2p.EVENTS.PEERS.CONNECTED.forEach(System.out::println);
            //p2p.EVENTS.PEERS.DISCONNECTED.forEach(System.out::println);
            //p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
            p2p.EVENTS.MSGS.ADDR.forEach(System.out::println);
            p2p.EVENTS.MSGS.INV.forEach(e -> {
                BitcoinMsg<InvMessage> msg = (BitcoinMsg<InvMessage>) e.getBtcMsg();
                List<InventoryVectorMsg> invVector = msg.getBody().getInvVectorList();
                long numTxs =
                        invVector.stream().filter( i -> i.getType().equals(InventoryVectorMsg.VectorType.MSG_TX)).count();
                long numBlocks =
                        invVector.stream().filter( i -> i.getType().equals(InventoryVectorMsg.VectorType.MSG_BLOCK)).count();
                System.out.println(" >> INV: " + numBlocks + " blocks, " + numTxs + " Txs");
            });
            p2p.EVENTS.STATE.HANDSHAKE.forEach(System.out::println);
            p2p.EVENTS.STATE.NETWORK.forEach(e -> {
                System.out.println(e);
                System.out.println("Threads:" + Thread.activeCount());
            });
            p2p.start();
            Thread.sleep(600_000);
            p2p.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Ignore
    public void quickstart() {

        P2P p2p = new P2PBuilder("testing").build();
        p2p.start();
        // Do something...
        p2p.stop();
    }
}
