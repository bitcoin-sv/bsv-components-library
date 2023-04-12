package io.bitcoinsv.bsvcl.net.protocol.wrapper;

import io.bitcoinsv.bitcoinjsv.params.Net;
import io.bitcoinsv.bitcoinjsv.params.RegTestParams;
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder;
import org.junit.jupiter.api.Test;

class P2PTest {

    /** Simple start stop of P2P component */
    @Test
    public void startStopTest() throws InterruptedException {
        ProtocolConfig config = ProtocolConfigBuilder.get(new RegTestParams(Net.REGTEST));
        P2P p2p = new P2PBuilder("testing")
                .config(config)
                .useLocalhost()
                .build();
        p2p.start();
        p2p.awaitStarted();
        p2p.stop();
        p2p.awaitStopped();
    }
}