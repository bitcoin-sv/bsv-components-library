package io.bitcoinsv.bsvcl.net.integration.protocol.wrapper.receivers


import io.bitcoinsv.bsvcl.tools.bigObjects.BigCollectionChunk
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectItemsReceivedEvent
import io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events.BigObjectReceivedEvent
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig
import io.bitcoinsv.bsvcl.tools.config.provided.RuntimeConfigDefault
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A testing class for the RawBlockReceiver.
 * This is an Integration test, so it connects to the real netwwork (Mainnet) and download Blocks from it.
 */
class RawBlockReceiver_BlocksDownloadTest extends Specification {

    // General Test Env Configuration:
    private static final Duration TEST_TIME_LIMIT = Duration.ofMinutes(2)

    // Basic Network Configuration:
    private static final int MIN_PEERS = 10
    private static final int MAX_PEERS = 15
    private static final int MAX_BLOCKS_PARALLEL = 6

    // Blocks to Download:
    private static final List<String> BLOCKS = Arrays.asList(
            "0000000000000000068cd5441b2f406562939988bfeafecfe9a90949c055ac78", // 1.6MB
            "000000000000000001e6ddb4461940f0bedf69c7b63a239f33d641c9f8e4ac73", // 1.4MB
            "0000000000000000032b2199242e73a7a1388f036648c49856d9d39b5e44d1b0", // 1.8MB
            "0000000000000000099ae699f1b233a5a5f8a33ce2a570850a86839dd831e4a6", // 840KB
            "0000000000000000031036042beb5a20085466f4640a61026ce849be411255dd", // 81K
            "000000000000000009b7423dc3ec7a97c6593bda228fcdce17e967f0e1729412", // 84KB
            "00000000000000000b53ca866c42c077d95b3d735df593229fd19f49352f5f80", // 17KB
            "0000000000000000087c125e472604bfbd43b524007a82439debe578c75f2ff0", // 266KB
            "0000000011139d059a772fb14123a0bbe66b1a6782d4aebe2a6b2c9f92850a7d", // 6MB
            "00000000000000000fc65b3827b997cbce18350b7aa03ac306367e220cb7ad52", // 115MB
            //"0000000000000000052c4236c4c34dc7686f8285e2646a584785b8d3b1eb8779", // 1.25GB
            "000000000000000002f5268d72f9c79f29bef494e350e58f624bcf28700a1846", // 369MB
            "0000000000000000027abeb2a2348dac5f953676f6b68a6ed5d92458a1c12cab", // 0.6MB
            "000000000000000000dd6c89655ca27fd2555247232a5ced8376f5bda0d26ec4", // 12MB

            "0000000000000000071e6e1c401fc530a63d27c826661a2f48709ba2ab51ecb4", // 7K
            "0000000000000000010b0c201f99c4636b35972fc870cdd322d49aea4e9e469e", // 4MB
            "00000000000000000dcb5ea5c87f337d017c077e10e314cb0176026266faef0c", // 17MB
            "0000000000000000039f4868d8c88d8ba86458101b965f5885cc63ed6814fb5c", // 2MB
            "00000000000000000c6e3e84fcf44f0305a2628d07bc082fd9885480c4ea0eb0", // 71MB
            "000000000000000002a8d922a4e1d365019758af5e9a2260f6cea0261d459b38", // 63MB
            "00000000000000000b03fc7421e1063e1f55e7a383801debc551daf6d37c3fa8", // 610KB
            "000000000000000009564c0360e55b125af1327eaf56b6b7566493112523437b", // 130KB
            "00000000000000000249f0276b4535875b497c42a737ce477b5c6e11ff55fcd3", // 9MB
            "000000000000000009b7423dc3ec7a97c6593bda228fcdce17e967f0e1729412", // 84MB
            "0000000000000000007ef109b1266d8701d15158e6795b7d8f2080ebecf3acaf", // 16MB
            "00000000000000000b53ca866c42c077d95b3d735df593229fd19f49352f5f80", // 10KB
            "00000000000000000b0f7d16e33e66b64bf94bb5c6543f3b680ce9d7162fef21", // 1.7MB
            "0000000000000000061757aed9f19d4e6a94ad5f309d1cc53f4303298cbf033f" // 2.2MB
    )

    private static final AtomicBoolean maxPeersReached = new AtomicBoolean(false)

    private static io.bitcoinsv.bsvcl.net.protocol.wrapper.receivers.RawBlockReceiver receiver;

    private void onBlockDownloaded(BigObjectReceivedEvent event) {
        println(">>>>>> BLOCK #" + event.getObjectId() + " :: downloaded from " + event.getSource() + " !")
        // We get an iterator and then we remove the whole Block:
        Iterator<BigCollectionChunk<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg>> chunksIt = receiver.getTxs(event.getObjectId());
        long txBytes = 0;
        long numTxs;
        while (chunksIt.hasNext()) {
            BigCollectionChunk<io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg> chunk = chunksIt.next()
            txBytes += chunk.items.stream().mapToLong({i -> i.lengthInBytes}).sum()
            numTxs += chunk.items.size();
        }
        receiver.remove(event.getObjectId())
        println(">>>>>> BLOCK #" + event.getObjectId() + " :: Removed. " + numTxs + " Txs [" + txBytes + " bytes].")
    }

    private void onTxsDownloaded(BigObjectItemsReceivedEvent event) {
        println(">>>>>> BLOCK #" + event.getObjectId() + " :: " + event.getItems().size() + " txs received from " + event.getSource() + " ...")
    }

    private void printBlocksState(io.bitcoinsv.bsvcl.net.network.events.HandlerStateEvent stateEvent) {
        if (maxPeersReached.get()) {
            io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerState blocksState = (io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerState) stateEvent.getState()
            println(blocksState.toString())
        }
    }

    private void printPeersState(io.bitcoinsv.bsvcl.net.network.events.HandlerStateEvent stateEvent) {
        if (!maxPeersReached.get()) {
            io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandlerState peersState = (io.bitcoinsv.bsvcl.net.protocol.handlers.handshake.HandshakeHandlerState) stateEvent.getState()
            println(peersState.toString())
        }
    }

    @Ignore
    def "Testing Block Receiver"() {
        given:
        // Network protocol:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig();

        // Runtime Config:
        RuntimeConfig runtimeConfig = new RuntimeConfigDefault().toBuilder()
                .msgSizeInBytesForRealTimeProcessing(5_000_000)
                .build()

        // Basic Config:
        io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .minPeers(OptionalInt.of(MIN_PEERS))
                .maxPeers(OptionalInt.of(MAX_PEERS))
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build()

        // Serialization Config:
        io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig messageConfig = config.getMessageConfig().toBuilder()
                .rawTxsEnabled(true)
                .build();

        // We set up the Download configuration:
        io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig blockConfig = config.getBlockDownloaderConfig().toBuilder()
                .maxBlocksInParallel(MAX_BLOCKS_PARALLEL)
                .removeBlockHistoryAfterDownload(false)
                .build()

        // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
        io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

        // We configure the P2P Service:
        io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P p2p = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("testing")
                .config(runtimeConfig)
                .config(config)
                .config(basicConfig)
                .config(messageConfig)
                .config(blockConfig)
                .config(discoveryConfig)
                .publishStates(Duration.ofMillis(500))
                .build()

        p2p.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({e -> maxPeersReached.set(true)})
        p2p.EVENTS.STATE.HANDSHAKE.forEach({stateEvent -> this.printPeersState(stateEvent)})
        p2p.EVENTS.STATE.BLOCKS.forEach({stateEvent -> this.printBlocksState(stateEvent)})

        // We crete our Receiver:
        receiver = new io.bitcoinsv.bsvcl.net.protocol.wrapper.receivers.RawBlockReceiver("testingRawBlockReceiver_blocksDownload", p2p)
        receiver.EVENTS.OBJECT_RECEIVED(5).forEach({e -> this.onBlockDownloaded(e)})
        receiver.EVENTS.ITEMS_RECEIVED(5).forEach({e -> this.onTxsDownloaded(e)})

        when:
        // We start the receiver & P2P activity:
        receiver.start()
        p2p.start()
        Instant beginTime = Instant.now()

        // After we reach the Max number of Peers, we start the downloading:
        while (!maxPeersReached.get()) { Thread.sleep(1000);}
        p2p.REQUESTS.BLOCKS.download(BLOCKS).submit()

        // We wait until the test is done OR ALL the blocks have been downoaded: // TODO :PENDING...
        while (Duration.between(beginTime, Instant.now()).compareTo(TEST_TIME_LIMIT) < 0) {
            Thread.sleep(1000) // 1 sec WAIT
        }

        // We Stop & clear everything:
        p2p.stop()
        receiver.destroy()

        then:
        true
    }
}