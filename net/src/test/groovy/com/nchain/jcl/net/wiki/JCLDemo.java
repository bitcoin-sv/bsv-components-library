package com.nchain.jcl.net.wiki;

import com.nchain.jcl.net.protocol.config.ProtocolConfig;
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import com.nchain.jcl.net.protocol.events.BlockTXsDownloadedEvent;
import com.nchain.jcl.net.protocol.events.PeerHandshakedEvent;
import com.nchain.jcl.net.protocol.wrapper.P2P;
import org.junit.Test;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-24
 */
public class JCLDemo {

    @Test
    public void test() {
        try {
            // JCL Demo...

            ProtocolConfig config = new ProtocolBSVMainConfig().toBuilder()
                    .minPeers(10)
                    .maxPeers(15)
                    .build();

            P2P p2p = P2P.builder("Demo")
                    .config(config)
                    .publishStates(Duration.ofSeconds(1))
                    .build();

            p2p.EVENTS.STATE.MESSAGES.forEach(System.out::println);

            p2p.startServer();

            Thread.sleep(5_000);

            p2p.stop();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onTxsDownloaded(BlockTXsDownloadedEvent event) {
        System.out.println(event.getTxsMsg().size() + " Txs downloaded from " + event.getBlockHeaderMsg().getHash().toString());

    }
}
