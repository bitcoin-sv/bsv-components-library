package com.nchain.jcl.net.temp

import com.nchain.jcl.base.tools.bytes.HEX
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper
import com.nchain.jcl.net.network.PeerAddress
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig
import com.nchain.jcl.net.protocol.handlers.message.MessagePreSerializer
import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg
import com.nchain.jcl.net.protocol.messages.GetHeadersMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.HeaderMsg
import com.nchain.jcl.net.protocol.messages.HeadersMsg
import com.nchain.jcl.net.protocol.messages.VarIntMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import org.junit.Test
import spock.lang.Specification

import java.time.Instant

class DelayTesting extends Specification {

    private GetHeadersMsg getHeaderFromHash(String hash, long protocolVersion){
        HashMsg hashMsg = HashMsg.builder().hash(HEX.decode(hash)).build();
        List<HashMsg> hashMsgs = Arrays.asList(hashMsg);

        BaseGetDataAndHeaderMsg baseGetDataAndHeaderMsg = BaseGetDataAndHeaderMsg.builder()
                .version(protocolVersion)
                .blockLocatorHash(hashMsgs)
                .hashCount(VarIntMsg.builder().value(1).build())
                .hashStop(HashMsg.builder().hash(Sha256Wrapper.ZERO_HASH.getBytes()).build())
                .build()

        GetHeadersMsg getHeadersMsg = GetHeadersMsg.builder()
                .baseGetDataAndHeaderMsg(baseGetDataAndHeaderMsg)
                .build()

        return getHeadersMsg
    }

    def "Testing DELAY"() {
        given:
        MessagePreSerializer preSerializer = new MessagePreSerializer() {
            @Override
            void processBeforeDeserialize(PeerAddress peerAddress, HeaderMsg headerMsg, byte[] msgBytes) {
                if (headerMsg.getCommand().equalsIgnoreCase("headers"))
                    System.out.println(Instant.now().toString() + " >> Header Bytes Received!!!!")
            }

            @Override
            void processAfterSerialize(PeerAddress peerAddress, HeaderMsg headerMSg, byte[] msgBytes) {
                // nothing here...
            }
        }
        String BLOCK_HASH_REF = "000000000000000001d956714215d96ffc00e0afda4cd0a96c96f8d802b1662b"
            ProtocolConfig config = new ProtocolBSVMainConfig().toBuilder()
                    .minPeers(9)
                    .maxPeers(10)
                    .build()
            MessageHandlerConfig messageConfig = config.messageConfig.toBuilder().preSerializer(preSerializer).build()
            P2P p2p = new P2PBuilder("testing")
                    .config(config)
                    .config(messageConfig)
                    .build()
            p2p.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e ->
                println("Minimum number of Peers reached...")
                Thread.sleep(10000)
                // We broadcast a GetHeaders Message...
                GetHeadersMsg getHeadersMsg = getHeaderFromHash(BLOCK_HASH_REF, config.basicConfig.protocolVersion)
                BitcoinMsg<GetHeadersMsg> btcMsg = new BitcoinMsgBuilder<>(config.basicConfig, getHeadersMsg).build()
                p2p.REQUESTS.MSGS.broadcast(btcMsg).submit()
                println(Instant.now().toString() + " > GetHeaders broadcasted")
            })
            p2p.EVENTS.MSGS.HEADERS.forEach({e ->
                println(Instant.now().toString() +  " > Getting a HEADERS Msg " + ((HeadersMsg) e.getBtcMsg().getBody()).count.toString() + " headers from " + e.peerAddress)
            })
            p2p.EVENTS.MSGS.ALL.forEach({e ->
                println(Instant.now().toString() + " > Incoming " + e.getBtcMsg().header.command.toUpperCase() + " from " + e.peerAddress);
            })
            p2p.EVENTS.MSGS.ALL_SENT.forEach({ e ->
                println(Instant.now().toString() +  " > Outcoming " + e.getBtcMsg().header.command.toUpperCase() + " to " + e.peerAddress);
            })
        when:
            p2p.start()
            Thread.sleep(20_000)
            p2p.stop()
        then:
            true
    }

    @Test
    def "testing genesisBlock"() {
        given:
            Sha256Wrapper blockHash = new ProtocolBSVMainConfig().genesisBlock.getHash()
        expect:
            blockHash.toString().equalsIgnoreCase("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")
    }
}
