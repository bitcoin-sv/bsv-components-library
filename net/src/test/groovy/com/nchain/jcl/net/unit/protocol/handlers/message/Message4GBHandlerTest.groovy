package com.nchain.jcl.net.unit.protocol.handlers.message

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.ProtocolVersion
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerConfig
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.BlockMsg
import com.nchain.jcl.net.protocol.messages.HashMsg
import com.nchain.jcl.net.protocol.messages.TxInputMsg
import com.nchain.jcl.net.protocol.messages.TxMsg
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg
import com.nchain.jcl.net.protocol.messages.TxOutputMsg
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerConfig
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import com.nchain.jcl.tools.config.RuntimeConfig
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinj.core.Sha256Hash
import io.bitcoinj.params.RegTestParams
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Testing class for Messages bigger than 4GB, which support has been added in the 70016 protocol upgrade
 */
class Message4GBHandlerTest extends Specification {

    // This property specified the threshold After which messages are considered "extended". In reality, this value is
    // 4GB. But if the local environment is limited in RAM and we can not afford to use that much memory to test, we
    // can "simulate" the "extended msgs" logic by using a lower value.

    private long BLOCK_SIZE_TO_TEST = 3_000_000_000L; // 3 GB

    // It builds a Big Block (extended message)
    private BlockMsg buildBigBlock() {
        HashMsg ZERO_HASH_MSG = new HashMsg.HashMsgBuilder().hash(Sha256Hash.ZERO_HASH.getBytes()).build()
        long currentSize = 0L;
        long numTxs = 0;
        List< TxMsg> txs = new ArrayList<>()
        while (currentSize < (BLOCK_SIZE_TO_TEST + 1_000_000)) { // 1  MB Buffer
            // We create a new Tx, just 1 input and 1 output:
            TxOutPointMsg outpointMsg = new TxOutPointMsg.TxOutPointMsgBuilder()
                .hash(ZERO_HASH_MSG)
                .index(1)
                .build();

            TxInputMsg txInMsg = new TxInputMsg.TxInputMsgBuilder()
                .signature_script(new byte[5_000_000])
                .sequence(1)
                .pre_outpoint(outpointMsg)
                .build()

            TxOutputMsg txOutMsg = new TxOutputMsg.TxOutputMsgBuilder()
                .pk_script(new byte[5_000_000])
                .txValue(5)
                .build()

            TxMsg tx = new TxMsg.TxMsgBuilder()
                .version(1)
                .lockTime(1)
                .tx_in(Arrays.asList(txInMsg))
                .tx_out(Arrays.asList(txOutMsg))
                .build()

            txs.add(tx)
            currentSize += tx.getLengthInBytes()
            numTxs++;
        } // while...

        // Block Header:
        BlockHeaderMsg headerMsg = new BlockHeaderMsg.BlockHeaderMsgBuilder()
                .prevBlockHash(ZERO_HASH_MSG)
                .hash(ZERO_HASH_MSG)
                .merkleRoot(ZERO_HASH_MSG)
                .transactionCount(numTxs)
                .build();

        // Whole Block:
        BlockMsg blockMsg = new BlockMsg.BlockMsgBuilder()
                .blockHeader(headerMsg)
                .transactionMsgs(txs)
                .build();

        return blockMsg;
    }

    /**
     * We use 2 JCL instances (2 PSP instances: server and client). We build a Big Block as an extended message and
     * then we send it from the client to the server, and check that the message is received properly by the Server.
     */
    @Ignore
    def "testing 4GBBlock"() {
        given:
            // Configuration:
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .maxNumThreadsForP2P(50)
                .build()
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(RegTestParams.get())
            ProtocolBasicConfig protocolBasicConfig = protocolConfig.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.SUPPORT_EXT_MSGS.getVersion())
                .thresholdSizeExtMsgs(BLOCK_SIZE_TO_TEST)
                .minPeers(OptionalInt.of(1))
                .maxPeers(OptionalInt.of(1))
                .build()
            MessageHandlerConfig messageHandlerConfig = protocolConfig.getMessageConfig()
            DeserializerConfig deserializerConfig = messageHandlerConfig.getDeserializerConfig().toBuilder()
                .partialSerializationMsgSize(10_000_000)
                .build()
            messageHandlerConfig = messageHandlerConfig.toBuilder().deserializerConfig(deserializerConfig).build()
            BlockDownloaderHandlerConfig downloadConfig = protocolConfig.getBlockDownloaderConfig().toBuilder()
                .maxIdleTimeout(Duration.ofSeconds(40))
                .build()

            // Server Configuration:
            P2P server = new P2PBuilder("server")
                .config(runtimeConfig)
                .config(protocolConfig)
                .config(protocolBasicConfig)
                .config(messageHandlerConfig)
                .config(downloadConfig)
                .publishState(BlockDownloaderHandler.HANDLER_ID, Duration.ofSeconds(5))
                .build()

            AtomicBoolean blockDownloaded = new AtomicBoolean(false)

            server.EVENTS.STATE.BLOCKS.forEach({ e -> println(e)})
            server.EVENTS.PEERS.HANDSHAKED.forEach({e ->
                println("SERVER >> " + e)
            })
            server.EVENTS.BLOCKS.BLOCK_HEADER_DOWNLOADED.forEach({ e ->
                println("SERVER >> BLOCK HEADER RECEIVED :: Msg Header: " + e.getBtcMsg().getHeader().toString() + " , BlockHeader Msg: " + e.getBtcMsg().getBody().getBlockHeader().toString())
            })
            server.EVENTS.BLOCKS.BLOCK_TXS_DOWNLOADED.forEach{e ->
                println("SERVER >> BATCH OF " +e.getBtcMsg().getBody().getTxs().size() + " Txs RECEIVED")
                Thread.sleep(30000) // WE simulate some work being done here
            }
            server.EVENTS.BLOCKS.BLOCK_DOWNLOADED.forEach({ e ->
                println("SERVER >> BLOCK DOWNLOADED")
                blockDownloaded.set(true)
            })

            // Client Configuration:
            P2P client = new P2PBuilder("client")
                .config(protocolConfig)
                .config(protocolBasicConfig)
                .build()

            client.EVENTS.MSGS.ALL.forEach({ e -> println("CLIENT >> " + e)})

        when:
            // We start both and connect them to each other:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(500)

            // Now we send a BIG Block from the Client to the Server, and we check that is being received properly:
            BlockMsg bigBlock = buildBigBlock();
            println(">>>  BLOCK OF " + bigBlock.getTransactionMsg().size() + " TXS AND " + ((int) (bigBlock.getLengthInBytes() / 1_000_000)) + " MBs created.")
            println(">>>  SERVER REQUESTING BLOCK FOM CLIENT...");
            server.REQUESTS.BLOCKS.download(Sha256Hash.ZERO_HASH.toString()).submit()
            Thread.sleep(100)
            println(">>>  CLIENT SERIALIZING AND SENDING BLOCK TO SERVER...");
            Instant begin = Instant.now()
            client.REQUESTS.MSGS.send(server.getPeerAddress(), bigBlock).submit()
            while (!blockDownloaded.get()) { Thread.sleep(100)}
            Duration duration = Duration.between(begin, Instant.now())
            println("Time to Serialize, send Big Block, receive it adn deserialize it: " + duration.toMillis() + " millisecs");
        then:
            blockDownloaded.get()
    }
}
