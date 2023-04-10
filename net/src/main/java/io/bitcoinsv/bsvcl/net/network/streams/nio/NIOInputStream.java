package io.bitcoinsv.bsvcl.net.network.streams.nio;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.config.NetworkConfig;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerInputStreamImpl;
import io.bitcoinsv.bsvcl.net.network.streams.StreamCloseEvent;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayStatic;
import io.bitcoinsv.bsvcl.tools.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.tools.thread.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class implements a PeerInputStream that receives ByteArrayReader objects and also works as a Source, so
 * it represents the final step in the Stream chain. It's physically connected to the REmote Peer we are receiving
 * the Bytes from, through a SocketChannel and a Selection Key.
 *
 * The process of Receiving Bytes from the Peer is as follows:
 * - The Selection Key detects that we hae new incoming data. This Key is managed by the HetowrkHandler.
 * - The NetworkHandler invokes the "readFromSocket()" method in this class.
 * - the "readFromSocket()" method reads data directly from the sockets, wraps it up into a ByteArrayReader, and invoke
 *   the "send()" method in this class, which will send that ByteArrayReader down the Stream to any other Stream that
 *   might be connected to this Stream (that will be a DeserializerStream).
 */
public class NIOInputStream extends PeerInputStreamImpl<ByteArrayReader, ByteArrayReader> implements PeerInputStream<ByteArrayReader> {

    // Configuration:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;

    // For loggin:
    private static final Logger log = LoggerFactory.getLogger(NIOInputStream.class);

    private final PeerAddress peerAddress;
    private final NIOStreamState state = new NIOStreamState();

    // The Selection Key and the Sockets linked to the physical connection to the remote Peer
    private final SelectionKey key;
    private final SocketChannel socketChannel;

    // The following variables control the capacity of the Buffer used to READ bytes from it.
    // sometimes, during the lifecycle of this class, it wil be used for regular communications (small
    // data) or for downloading big amounts of info (like downloading a full block). In the later case, using
    // a bigger capacity will increase performance drastically, so in that case we need to re-create again the
    // Buffer but using a higher capacity. The NIOSocketStream can switch from normal/high capacity
    // at any time, so the following variables keep track of the capacities and the flags that indicate when to
    // switch:

    private ByteBuffer readBuffer;
    private boolean bufferNeedToUpgrade;
    private boolean bufferNeedToReset;
    private final int bufferNormalCapacity;
    private final int bufferHighCapacity;

    private final int QUEUE_SIZE = 100;
    private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);

    /**
     * This executor is used to release socket channel reading thread from deserializing flow.
     * It uses blocking queue, so it doesn't read more than deserializer is able to process.
     */
    private final ExecutorService executorService = ThreadUtils.getBlockingSingleThreadExecutorService("NIOInputStream_executor", Thread.NORM_PRIORITY, queue);

    public NIOInputStream(PeerAddress peerAddress,
                          RuntimeConfig runtimeConfig,
                          NetworkConfig networkConfig,
                          SelectionKey key) {
        super(peerAddress, null);

        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
        this.bufferNormalCapacity = Math.min(Math.max(networkConfig.getMaxMessageSizeAvgInBytes(),
                networkConfig.getNioBufferSizeLowerBound()), networkConfig.getNioBufferSizeUpperBound());
        this.bufferHighCapacity = networkConfig.getNioBufferSizeUpgrade();

        this.readBuffer = getBufferForReading();
    }

    // It marks a flag saying that before using the Buffer next time, we need to upgrade it
    // (we can NOT just do it right now, since it might be used at this moment)

    public void upgradeBufferSize() {
        bufferNeedToUpgrade = true;
        bufferNeedToReset = false;
    }

    // It marks a flag saying that before using the Buffer next time, we need to reset it
    // (we can NOT just do it right now, since it might be used at this moment)

    public void resetBufferSize() {
        bufferNeedToUpgrade = false;
        bufferNeedToReset = true;
    }

    // Returns the ByteBuffer used to read the data from the socket. Sometimes, during the lifecycle of
    // this stream, it might need to read more data than usual (like when downloading a Block), in that
    // case, this method will return a bigger buffer...

    private ByteBuffer getBufferForReading() {
        ByteBuffer result = null;
        if (bufferNeedToUpgrade) {
            log.trace("upgrading Buffer...");
            result = ByteBuffer.allocateDirect(bufferHighCapacity);
        }  else if (bufferNeedToReset) {
            log.trace("resetting Buffer...");
            result = ByteBuffer.allocateDirect(bufferNormalCapacity);
        }  else if (readBuffer == null) {
            log.trace("creating Buffer...");
            result = ByteBuffer.allocateDirect(bufferNormalCapacity);
        }  else result = this.readBuffer;

        this.readBuffer = result;
        bufferNeedToUpgrade = false;
        bufferNeedToReset = false;
        return result;
    }

    private void updateState(int bytesReceivedToAdd) {
        state.increment(bytesReceivedToAdd);
    }

    public synchronized int readFromSocket() throws IOException {
        if (queue.size() == QUEUE_SIZE) {
            return 0;
        }

        // We read data from the Buffer and connection verifications:
        try {
            ByteBuffer buffer = getBufferForReading();
            int read = this.socketChannel.read(buffer);
            updateState(read);

            if (read <= 0) {
                return read;
            }

            // We feed the StreamOperations with that data. We concert the data into a ByteArray object, and we
            // feed our StreamOperations with it:
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data,0, data.length);
            buffer.compact();

            // We send this data down the Stream:
            // This data needs to be wrapped up in a ByteArray and then in a ByteArrayReader. But when it arrives at the
            // destination (which is a DeserializerStream), this reader will only be used to GET all its content
            // "getFullContent()", which in turns calls the "get()" method in the ByteArray. And after that, this reader
            // and the underlying ByteArray won't be used anymore.
            // So we are using here an "improved" version of ByteArray which is optimized for the situation when we are
            // mainly only interested in its "get()" method.


            executorService.execute(() -> {
                ByteArrayReader byteArrayReader = new ByteArrayReader(new ByteArrayStatic(data)); // Optimization
                onDataListeners.forEach(dataListener -> dataListener.accept(byteArrayReader));
            });

            return read;
        } catch (IOException e) {
            log.error("Reading form {} socket failed!", peerAddress, e);
            close(new StreamCloseEvent());
            throw e;
        }
    }

    @Override
    public void close(StreamCloseEvent event) {
        try {
            super.close(event);
            key.cancel();
            this.socketChannel.close();
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        }
    }

    @Override
    public void expectedMessageSize(long messageSize) {
        boolean isABigMessage = (messageSize >= this.bufferHighCapacity);
        if (isABigMessage && this.readBuffer.capacity() < this.bufferHighCapacity) {
            this.upgradeBufferSize();
        } else if ( !isABigMessage && this.readBuffer.capacity() > this.bufferNormalCapacity) {
            this.resetBufferSize();
        }
    }

    @Override
    public  List<ByteArrayReader> transform(ByteArrayReader dataEvent) {
        throw new UnsupportedOperationException();
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public NIOStreamState getState() {
        return this.state;
    }
}