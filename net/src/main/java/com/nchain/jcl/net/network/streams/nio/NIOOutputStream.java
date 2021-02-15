package com.nchain.jcl.net.network.streams.nio;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.network.config.NetworkConfig;

import com.nchain.jcl.net.network.streams.PeerOutputStream;
import com.nchain.jcl.net.network.streams.PeerOutputStreamImpl;
import com.nchain.jcl.net.network.streams.StreamCloseEvent;
import com.nchain.jcl.net.network.streams.StreamDataEvent;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.log.LoggerUtil;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Implementation of a Destination for an NIOOutputStream.
 * This class represents the Destination, so this is the last point in the Stream chain where the
 * data is sent.
 * This class is physically connected to the remote Peer through a SocketChannel and
 * a SelectionKey that is used to send data to it. The Selection Key is also being managed by the
 * NetworkHandler class. So the process os writting/sending dat to the Remote Peer is this:
 *
 * - Some bytes are being sent through the "send()" method.
 * - These bytes are immediately kept in a Collection, to keep track of them.
 * - We update the Selection Key, to notify it tht we are ready for writing.
 * - The NetworkHandler class, which is monitoring the Selection Key all the time, will detect this
 *   and will invoke the "writeToSocket()" method in this class.
 * - The "writeToSocket()" method will take the bytes we collected in the first step, and will try to
 *   write them into the Socket connected to the Remote Peer.
 *
 */

public class NIOOutputStream extends PeerOutputStreamImpl<ByteArrayReader, ByteArrayReader> implements PeerOutputStream<ByteArrayReader> {

    // Configuration:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;

    // For loggin:
    LoggerUtil logger;

    private PeerAddress peerAddress;
    private NIOStreamState state;

    // The Selection Key and the Sockets linked to the physical connection to the remote Peer
    private SelectionKey key;
    private SocketChannel socketChannel;

    // When using NIO and Buffers, there is no guarantee that all the bytes are written to the Buffer. Sometimes you
    // write 10 bytes but only 7 have been actually written. So we need to keep track of the bytes pending
    // to write:
    private long bytesToWriteRemaining = 0;
    // Here we keep the bytes pending to be written to the Socket:
    private Queue<ByteBuffer> buffersToWrite = new ConcurrentLinkedQueue<>();

    public NIOOutputStream(PeerAddress peerAddress,
                           ExecutorService executor,
                           RuntimeConfig runtimeConfig,
                           NetworkConfig networkConfig,
                           SelectionKey key) {
        super(peerAddress, executor, null);
        this.logger = new LoggerUtil(peerAddress.toString(), this.getClass());

        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();

        this.state = NIOStreamState.builder().build();

    }

    @Override
    public List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<ByteArrayReader> data) {
        throw new UnsupportedOperationException();
    }

    private void updateState(int bytesSentToAdd) {
        this.state.toBuilder()
                .numBytesProcessed(state.getNumBytesProcessed().add(BigInteger.valueOf(bytesSentToAdd)))
                .build();
    }

    public void send(StreamDataEvent<ByteArrayReader> event) {
        //logger.trace("Sending " + event.getData().size() + " bytes : " + HEX.encode(event.getData().getFullContent()));
        // We get all the data from this Reader and we add it to the buffer of ByteBuffers.
        bytesToWriteRemaining += event.getData().size();
        buffersToWrite.offer(ByteBuffer.wrap(event.getData().getFullContentAndClose())); // TODO: CAREFUL
        notifyChannelWritable();
    }

    public void close(StreamCloseEvent event) {
        logger.trace("Closing Stream...");
        key.cancel();
    }

    private void notifyChannelWritable() {
        try {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        } catch (CancelledKeyException e) {
            logger.debug("Trying to send byte to " + peerAddress + ", but the Key is Cancelled...");
        } catch (Exception e) {
            logger.debug("Trying to send byte to " + peerAddress + ", but an Exception was thrown " + e.getMessage());
        }
    }

    private void notifyChannelNotWritable() {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        // Don't bother waking up the selector here, since we're just removing an op, not adding
    }


    public int writeToSocket() throws IOException {
        int writeResult = 0;
        Iterator<ByteBuffer> buffersToWriteIterator = buffersToWrite.iterator();
        while (buffersToWriteIterator.hasNext()) {
            ByteBuffer writeBuffer = buffersToWriteIterator.next();
            int numBytesWritten = socketChannel.write(writeBuffer);
            updateState(numBytesWritten);
            writeResult += numBytesWritten;
            bytesToWriteRemaining -= numBytesWritten;

            if (!writeBuffer.hasRemaining())  buffersToWriteIterator.remove();
            else break;

        } // while...
        if (buffersToWrite.isEmpty()) notifyChannelNotWritable();
        //logger.debug(writeResult + " bytes sent to " + socketChannel.socket().getRemoteSocketAddress());
        return writeResult;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public NIOStreamState getState() {
        return this.state;
    }
}
