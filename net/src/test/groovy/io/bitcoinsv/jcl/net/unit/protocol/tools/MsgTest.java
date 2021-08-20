/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.tools;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.jcl.net.network.streams.StreamState;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig;
import io.bitcoinsv.jcl.net.protocol.config.provided.ProtocolBSVMainConfig;
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VersionAckMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.unit.network.streams.PeerStreamInOutSimulator;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;

import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 17:06
 *
 * An utility class that provides methods to generate Bitcoin Messages so they can be used for testing
 */
public class MsgTest {

    public static class DummyPeerStreamSource extends PeerStreamInOutSimulator<ByteArrayReader> implements PeerInputStream<ByteArrayReader> {
        DummyPeerStreamSource(ExecutorService executor) { super( null, executor);}
        public PeerAddress getPeerAddress() { try {return PeerAddress.localhost(8111);} catch (Exception e) {e.printStackTrace(); return null;}}
        public StreamState getState() { return null; }
    }

    public static class DummyPeerDelayStreamSource extends ByteReaderDelaySource implements PeerInputStream<ByteArrayReader> {
        public DummyPeerDelayStreamSource(ExecutorService executor, int bytesPerSec) { super(executor, bytesPerSec);}
        public PeerAddress getPeerAddress() { try {return PeerAddress.localhost(8111);} catch (Exception e) {e.printStackTrace(); return null;}}
        public StreamState getState() { return null; }
    }


    // Definition of some Messages:
    public final static String ADDR_MSG_HEX         = "e3e1f3e86164647200000000000000001f00000077b42c0c010b6d2f5d000000000000000000000000000000000000ffff7f000001208d";
    public final static String ADDR_BODY_HEX        = "010b6d2f5d000000000000000000000000000000000000ffff7f000001208d";
    public final static String PING_MSG_HEX         = "e3e1f3e870696e6700000000000000000800000032ab095c3d9a9cb22d32b40b";
    public final static String IGNORE_MSG_HEX       = "e3e1f3e870696e670aa00000000000000800000032ab095c3d9a9cb22d32b40b";
    public final static String INV_MSG_HEX          = "e3e1f3e8696e7600000000000000000025000000e27152ce0101000000a69d45e7abc3b8fc363d13b88aaa2f2ec62bf77b6881e8bd7bd1012fd81d802b";
    public final static String BLOCK_MSG_HEX        = "e3e1f3e8426c6f636b000000000000007f010000dd4043ed0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000";
    public final static String BLOCK_BODY_HEX       = "0100000040f11b68435988807d64dff20261f7d9827825fbb37542601fb94d45000000005d0a2717cccfb28565e04baf2708f32068fb80f98765210ce6247b8939ab2012ecd9d24c1844011d00d3610502010000000100000000000000000000000000000000000000000000000000000000000000001a00000007041844011d0142ffffffff0100f2052a010000004104a313febd5f91b6a13bd9c5317030518fee96d1319a0eb10076917294933d09c17dc1588a06953a264738f2acea0c66b99e796caa4f28158e0dd5f6fed69a185b000000000100000001aa18a952c3f73e5d7440bc570b2aa78f72059887b25b6a1790514b7feedec090000000004104ac44bdf511477465cb70fef1d06b9241e74d26047ccbdfa641ec9a0115ad35594cbb58a61a6fd56893a405bcffbf6555995ddedc7e6cd4e5ceb83a37e1cf8f98ffffffff02004d92d86a00000014b8083945473bc8289efb681f94de7b07a5b851ad00743ba40b00000014ef01911c9efec6799d1ee5f7c6fb072d9669da8000000000";

    public final static String VERSION_ACK_HEX      = "e3e1f3e876657261636b000000000000000000005df6e0e2";

    public static BitcoinMsg<AddrMsg> getAddrMsg() {
        ProtocolConfig config = new ProtocolBSVMainConfig();
        DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .maxBytesToRead((long) (ADDR_BODY_HEX.length()/2))
                .build();
        ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(ADDR_MSG_HEX));
        BitcoinMsg<AddrMsg> result = BitcoinMsgSerializerImpl.getInstance().deserialize(context, byteReader, AddrMsg.MESSAGE_TYPE);
        return result;
    }

    public static BitcoinMsg<VersionAckMsg> getVersionAckMsg() {
        ProtocolConfig config = new ProtocolBSVMainConfig();
        DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .maxBytesToRead((long) (VERSION_ACK_HEX.length()/2))
                .build();
        ByteArrayReader byteReader = new ByteArrayReader(Utils.HEX.decode(VERSION_ACK_HEX));
        BitcoinMsg<VersionAckMsg> result = BitcoinMsgSerializerImpl.getInstance().deserialize(context, byteReader, VersionAckMsg.MESSAGE_TYPE);
        return result;
    }

    public static PeerInputStream<ByteArrayReader> getDummyStreamSource() {
        return new DummyPeerStreamSource(Executors.newSingleThreadExecutor());
    }

    public static PeerInputStream<ByteArrayReader> getDummyDelayStreamSource(int delay) {
        return new DummyPeerDelayStreamSource(Executors.newSingleThreadExecutor(), delay);
    }

}
