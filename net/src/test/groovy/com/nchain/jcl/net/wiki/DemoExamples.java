package com.nchain.jcl.net.wiki;

import com.nchain.jcl.net.protocol.config.ProtocolConfig;
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import com.nchain.jcl.net.protocol.config.provided.ProtocolBTCMainConfig;
import com.nchain.jcl.net.protocol.events.PeerHandshakedEvent;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import com.nchain.jcl.net.protocol.wrapper.P2P;

import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-23
 */
@Ignore
public class DemoExamples {

    @Test
    public void testBasicConnection() throws Exception {
        P2P p2p = P2P.builder("demo").build();
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    @Test
    public void testBasicStreaming() throws Exception {
        P2P p2p = P2P.builder("demo").build();
        p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    @Test
    public void testStreamingWithMethod() throws Exception {
        P2P p2p = P2P.builder("demo").build();
        p2p.EVENTS.PEERS.HANDSHAKED.forEach(this::processPeerHandshaked);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    private void processPeerHandshaked(PeerHandshakedEvent event) {
        System.out.println("Peer handshaked: " + event.getPeerAddress());
    }


    @Test
    public void testFilters() throws Exception {
        P2P p2p = P2P.builder("demo").build();
        p2p.EVENTS.PEERS.HANDSHAKED
                .filter(e -> e.getPeerAddress().toString().startsWith("18"))
                .forEach(System.out::println);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }


    @Test
    public void testStramingMosgs() throws Exception {
        P2P p2p = P2P.builder("demo").build();
        p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
        p2p.EVENTS.MSGS.ALL
                .filter(e -> e.getBtcMsg().is(VersionMsg.MESSAGE_TYPE))
                .forEach(System.out::println);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    @Test
    public void configuration1() throws Exception {
        ProtocolConfig p2pConfig = new ProtocolBSVMainConfig();
        ProtocolConfig p2pConfig2 = new ProtocolBTCMainConfig();
        P2P p2p = P2P.builder("demo").config(p2pConfig2).build();
        p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    @Test
    public void configurationBasicChanges() throws Exception {
        ProtocolConfig p2pConfig = new ProtocolBSVMainConfig().toBuilder()
                .minPeers(10)
                .maxPeers(20)
                .build();
        P2P p2p = P2P.builder("demo")
                .config(p2pConfig)
                .build();
        p2p.EVENTS.PEERS.HANDSHAKED.forEach(System.out::println);
        p2p.start();
        Thread.sleep(5_000);
        p2p.stop();
    }

    @Test
    public void downloadBlocks() throws Exception {
        ProtocolConfig p2pConfig = new ProtocolBSVMainConfig().toBuilder()
                .minPeers(10)
                .maxPeers(15)
                .build();
        P2P p2p = P2P.builder("demo")
                .config(p2pConfig)
                .publishStates(Duration.ofSeconds(1))
                .build();
        p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED
                .forEach(e -> System.out.println("Block downloaded: " + e.getBlockHeader().getHash().toString()));
        //p2p.EVENTS.BLOCKS.BLOCK_TXS_DOWNLOADED
        //        .forEach(e -> System.out.println( " > " + e.getTxsMsg().size() + " Txs downloaded from " + e.getBlockHeaderMsg().getHash()));
        p2p.EVENTS.BLOCKS.BLOCK_DOWNLOADED
                .filter(e -> e.getBlockHeader().getHash().toString().endsWith("4814d7"))
                .forEach(e -> System.out.println("Small Block downloaded!!!"));
        p2p.EVENTS.STATE.BLOCKS.forEach(System.out::println);
        p2p.start();
        p2p.REQUESTS.BLOCKS
                .download(Arrays.asList(
                        "000000000000000001995c8df10190b45820135605957ac639fb85535d248dca",
                        "000000000000000000a6b934bdb833e4dec73cc2c1386b3b9b538dfc604814d7",
                        "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846"))
                .submit();
        Thread.sleep(60_000);
        p2p.stop();

    }
}
