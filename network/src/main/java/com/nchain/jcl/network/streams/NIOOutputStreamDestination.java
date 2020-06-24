package com.nchain.jcl.network.streams;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.PeerStream;
import com.nchain.jcl.network.config.NetworkConfig;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.streams.OutputStreamDestinationImpl;
import com.nchain.jcl.tools.streams.StreamCloseEvent;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import lombok.Getter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 14:09
 */
public class NIOOutputStreamDestination extends OutputStreamDestinationImpl<ByteArrayReader> implements PeerStream {

    // Configuration:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;

    @Getter
    private PeerAddress peerAddress;
    @Getter
    private NIOOutputStreamState state;

    // The Selection Key and the Sockets linked to the physical connection to the remote Peer
    private SelectionKey key;
    private SocketChannel socketChannel;

    // When using NIO and Buffers, there is no guarantee that all the bytes are written to the Buffer. Sometimes you
    // write 10 bytes but only 7 have been actually written. So we need to keep track of the bytes pending
    // to write:
    private long bytesToWriteRemaining = 0;
    private Queue<ByteBuffer> buffersToWrite = new ConcurrentLinkedQueue<>();

    public NIOOutputStreamDestination(ExecutorService executor,
                                      RuntimeConfig runtimeConfig,
                                      NetworkConfig networkConfig,
                                      PeerAddress peerAddress,
                                      SelectionKey key) {
        super(executor);
        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
        this.onData(this::onData);
        this.onClose(this::onClose);

    }

    private void updateState(int bytesSentToAdd) {
        this.state.toBuilder()
                .numBytesSent(state.getNumBytesSent().add(BigInteger.valueOf(bytesSentToAdd)))
                .build();
    }

    public void onData(StreamDataEvent<ByteArrayReader> event) {
        // We get all the data from this Reader and we add it to the buffer of ByteBuffers.
        bytesToWriteRemaining += event.getData().size();
        buffersToWrite.offer(ByteBuffer.wrap(event.getData().getFullContentAndClose())); // TODO: CAREFUL
        notifyChannelWritable();
    }

    public void onClose(StreamCloseEvent event) {
        key.cancel();
    }

    private void notifyChannelWritable() {
        try {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        } catch (CancelledKeyException e) {
            //logger.debug("Trying to send byte to " + peerAddress + ", but the Key is Cancelled...");
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

}
