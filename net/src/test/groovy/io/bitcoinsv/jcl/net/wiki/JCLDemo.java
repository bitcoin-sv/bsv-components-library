/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.wiki;

import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.jcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.jcl.net.protocol.streams.deserializer.DeserializerConfig;
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-24
 */
@Ignore
public class JCLDemo {

    @Test
    public void test() {
        try {
            // JCL Demo...

            ProtocolConfig config = new ProtocolBSVMainConfig().toBuilder()
                    .minPeers(10)
                    .maxPeers(15)
                    .build();

            // WE enable the Deserializer Cache Stats...
            MessageHandlerConfig messageHandlerConfig = config.getMessageConfig();
            DeserializerConfig deserializerCacheConfig = messageHandlerConfig.getDeserializerConfig()
                    .toBuilder()
                    .generateStats(true)
                    .build();
            messageHandlerConfig = messageHandlerConfig.toBuilder().deserializerConfig(deserializerCacheConfig).build();

            P2P p2p = P2P.builder("Demo")
                    .config(config)
                    .config(messageHandlerConfig)
                    .publishStates(Duration.ofSeconds(1))
                    .build();

            p2p.EVENTS.STATE.MESSAGES.forEach(System.out::println);
            p2p.EVENTS.MSGS.ALL.forEach(e -> System.out.println(e.getBtcMsg()));
            p2p.startServer();

            Thread.sleep(5_000);

            p2p.stop();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
