package io.bitcoinsv.bsvcl.net.network.streams.nio;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.network.config.NetworkConfig;
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStream;
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStreamImpl;
import io.bitcoinsv.bsvcl.net.network.streams.IStreamHolder;
import io.bitcoinsv.bsvcl.net.network.streams.StreamCloseEvent;
import io.bitcoinsv.bsvcl.common.writebuffer.WriteBuffer;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * Implementation of a Destination for an NIOOutputStream.
 * This class represents the Destination, so this is the last point in the Stream chain where the
 * data is sent.
 * This class is physically connected to the remote Peer through a SocketChannel and
 * a SelectionKey that is used to send data to it. The Selection Key is also being managed by the
 * NetworkHandler class. So the process os writting/sending dat to the Remote Peer is this:
 * <p>
 * - Some bytes are being sent through the "send()" method.
 * - These bytes are immediately kept in a Collection, to keep track of them.
 * - We update the Selection Key, to notify it tht we are ready for writing.
 * - The NetworkHandler class, which is monitoring the Selection Key all the time, will detect this
 * and will invoke the "writeToSocket()" method in this class.
 * - The "writeToSocket()" method will take the bytes we collected in the first step, and will try to
 * write them into the Socket connected to the Remote Peer.
 */

public class NIOOutputStream extends PeerOutputStreamImpl<ByteArrayReader, ByteArrayReader> implements PeerOutputStream<ByteArrayReader> {

    private final NIOStreamState state = new NIOStreamState();
    // For loggin:
    private static final Logger log = LoggerFactory.getLogger(NIOOutputStream.class);
    // Configuration:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;
    private PeerAddress peerAddress;
    // The Selection Key and the Sockets linked to the physical connection to the remote Peer
    private SelectionKey key;
    private SocketChannel socketChannel;

    private final ReentrantLock streamerLock = new ReentrantLock();
    private final NIOStreamerHolder streamer;

    public NIOOutputStream(
        PeerAddress peerAddress,
        RuntimeConfig runtimeConfig,
        NetworkConfig networkConfig,
        SelectionKey key
    ) {
        super(peerAddress, null);

        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();

        streamer = new NIOStreamerHolder(new WriteBuffer(peerAddress.toString(), runtimeConfig.getWriteBufferConfig(), socketChannel));
    }

    @Override
    public List<ByteArrayReader> transform(ByteArrayReader data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(ByteArrayReader event) {
        stream(s -> s.send(event));
    }

    @Override
    public void close(StreamCloseEvent event) {
        log.trace("Closing Stream...");
        streamer.close();
        key.cancel();
    }

    @Override
    public void stream(Consumer<IStreamHolder<ByteArrayReader>> streamer) {
        streamerLock.lock();
        streamer.accept(this.streamer);
        streamerLock.unlock();
    }

    public synchronized int writeToSocket() throws IOException {
        int numBytesWritten = streamer.extract();
        updateState(numBytesWritten);
        return numBytesWritten;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public NIOStreamState getState() {
        return this.state;
    }


    private void updateState(int bytesSentToAdd) {
        state.increment(bytesSentToAdd);
    }
}