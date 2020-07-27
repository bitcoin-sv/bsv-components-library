package com.nchain.jcl.network.streams.nio;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.network.streams.PeerInputStream;
import com.nchain.jcl.network.streams.PeerStreamInfo;
import com.nchain.jcl.network.config.NetworkConfig;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.HEX;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.streams.InputStreamSourceImpl;
import com.nchain.jcl.tools.streams.StreamCloseEvent;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-22 13:12
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
public class NIOInputStreamSource extends InputStreamSourceImpl<ByteArrayReader> implements PeerInputStream<ByteArrayReader> {

    // Configuration:
    private RuntimeConfig runtimeConfig;
    private NetworkConfig networkConfig;

    // For loggin:
    LoggerUtil logger;

    @Getter
    private PeerAddress peerAddress;
    @Getter
    private NIOInputStreamState state;

    // The Selection Key and the Sockets linked to the physical connection to the remote Peer
    private SelectionKey key;
    private SocketChannel socketChannel;

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
    private int bufferNormalCapacity;
    private int bufferHighCapacity;

    public NIOInputStreamSource(ExecutorService executor,
                                RuntimeConfig runtimeConfig,
                                NetworkConfig networkConfig,
                                PeerAddress peerAddress,
                                SelectionKey key) {
        super(executor);
        this.logger = new LoggerUtil(peerAddress.toString(), this.getClass());

        this.runtimeConfig = runtimeConfig;
        this.networkConfig = networkConfig;
        this.peerAddress = peerAddress;
        this.key = key;
        this.socketChannel = (SocketChannel) key.channel();
        this.bufferNormalCapacity = Math.min(Math.max(networkConfig.getMaxMessageSizeAvgInBytes(),
                networkConfig.getNioBufferSizeLowerBound()), networkConfig.getNioBufferSizeUpperBound());
        this.bufferHighCapacity = networkConfig.getNioBufferSizeUpgrade();

        this.readBuffer = getBufferForReading();
        this.state = NIOInputStreamState.builder().build();

    }

    private void updateState(int bytesReceivedToAdd) {
        this.state.toBuilder()
                .numBytesReceived(state.getNumBytesReceived().add(BigInteger.valueOf(bytesReceivedToAdd)))
                .build();
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
            logger.trace("upgrading Buffer...");
            result = ByteBuffer.allocateDirect(bufferHighCapacity);
        }  else if (bufferNeedToReset) {
            logger.trace("resetting Buffer...");
            result = ByteBuffer.allocateDirect(bufferNormalCapacity);
        }  else if (readBuffer == null) {
            logger.trace("creating Buffer...");
            result = ByteBuffer.allocateDirect(bufferNormalCapacity);
        }  else result = this.readBuffer;

        this.readBuffer = result;
        bufferNeedToUpgrade = false;
        bufferNeedToReset = false;
        return result;
    }

    public int readFromSocket() throws IOException {
        // We read data from the Buffer and connection verifications:
        try {
            // Before using the Buffer to read data from it, we check if we need to upgrtade/reset it...
            ByteBuffer buffer = getBufferForReading();
            int read = this.socketChannel.read(buffer);
            updateState(read);

            //logger.debug(read + " bytes received from " +this.socketChannel.socket().getRemoteSocketAddress());
            if (read <= 0) return read;

            // We feed the StreamOperations with that data. We concert the data into a ByteArray object, and we
            // feed our StreamOperations with it:
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data,0, data.length);
            buffer.compact();

            // We send this data down the Stream:
            ByteArrayReader byteArrayReader = new ByteArrayReader(data);
            //logger.debug(read + " bytes received : " + HEX.encode(byteArrayReader.getFullContent()));
            this.send(new StreamDataEvent<>(byteArrayReader));
            return read;
        } catch (IOException ioe) {
            this.close(new StreamCloseEvent());
        }
        return -1;
    }

    @Override
    public void close(StreamCloseEvent event) {
        super.close(event);
        key.cancel();
    }
}
