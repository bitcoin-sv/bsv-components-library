package com.nchain.jcl.protocol.handlers.block;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.messages.BlockMsg;
import com.nchain.jcl.protocol.streams.DeserializerStream;
import com.nchain.jcl.protocol.streams.DeserializerStreamState;
import lombok.Getter;
import lombok.Setter;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:23
 *
 * This class stores all the info the blockDownloader Handler needs to keep track of for each Peer, in order
 * to manage the download of blocks.
 */
public class BlockPeerInfo {

    /**
     * Definition of the differetn States a Peer is regarding its connection. When a Peer is disconnected, its
     * info is NOT removed, instead its kept so we can "remember" some parameters about it like the download speed.
     * So we keep information about Peers even though they are disconnected, so we need this state to differentiate
     * them from the active ones.
     */
    enum PeerConnectionState {
        CONNECTED, HANDSHAKED, DISCONNECTED;
    }

    /**
     * Definition of the different states a Peer might be during Block downloading.
     */
    public enum PeerWorkingState {
        IDLE,                           // Peer doing nothing. Default
        PROCESSING;
    }

    /**
     * Definition of Info about the Current Block being downloaded by this Peer.
     */
    @Getter
    public class BlockProgressInfo {
        protected String hash;
        protected PeerAddress peerAddress; // a bit redundant, but it's ok
        protected boolean corrupted;
        protected Long bytesTotal;
        protected Long bytesDownloaded;
        protected Boolean realTimeProcessing;
        protected Instant startTimestamp;
        protected Instant lastBytesReceivedTimestamp;

        public BlockProgressInfo(String hash, PeerAddress peerAddress) {
            this.hash = hash;
            this.peerAddress = peerAddress;
            this.startTimestamp = Instant.now();
            this.lastBytesReceivedTimestamp = Instant.now();
        }

        @Override
        public String toString() {
            StringBuffer result = new StringBuffer("Block status: ");
            result.append("Hash: " + hash + ", ");
            result.append(" Peer: " + peerAddress + ",");
            if (corrupted) result.append("CORRUPTED");
            else {
                DecimalFormat format = new DecimalFormat("#0.0");
                String bytesTotalStr = (bytesTotal == null) ? "¿? MB " : format.format((double) bytesTotal / 1_000_000) + " MB";
                String bytesDownStr =  (bytesDownloaded == null) ? "¿? MB " : format.format((double) bytesDownloaded / 1_000_000) + " MB";
                String progressStr = (bytesTotal == null) ? "¿? %" : (int) (bytesDownloaded * 100 / bytesTotal) + " %";
                result.append("progress: " + progressStr);
                result.append(" [" + bytesDownStr + " / " + bytesTotalStr + "]");
            }
            return result.toString();
        }
    }

    // A comparator that orders the Peers by Speed (high speed first)
    public static final Comparator<BlockPeerInfo> SPEED_COMPARATOR = (peerA, peerB) -> peerB.downloadSpeed - peerA.downloadSpeed;


    // Peer Info:
    @Getter private PeerAddress peerAddress;
    @Getter private PeerConnectionState connectionState;
    @Getter private PeerWorkingState workingState;
    @Getter private Integer downloadSpeed; // bytes/sec

    // A reference to the Deserializer Stream used by this Peer:
    @Getter private DeserializerStream stream;

    // Info bout the Block being currently downloaded by this Peer:
    @Getter private BlockProgressInfo currentBlockInfo;


    /** Constructor */
    public BlockPeerInfo(PeerAddress peerAddress, DeserializerStream stream) {
        this.peerAddress = peerAddress;
        this.stream = stream;
        this.connectionState = PeerConnectionState.CONNECTED;
        this.workingState = PeerWorkingState.IDLE;
        // When a new Peer is connected, we set its downloading Speed to MAX_INTEGER instead of 0, to give them higher
        // priority than those Peers who have speed zero. That way we garantee that all new Peers connected will have a
        // chance to probe their speed.
        this.downloadSpeed = Integer.MAX_VALUE;
    }

    /**
     * It resets the peer, to make it ready to download a new Block. This Peer might have been used to downloadad
     * another Block previously, so every time we reset it, we reset the properties related to the current
     * download, but some other "global" variabels are kept, like the download Speed
     */
    public void reset() {
        this.workingState = PeerWorkingState.IDLE;
        this.currentBlockInfo = null;
    }

    /** It updates the Peer to reflect that it's just connected */
    public void connect(DeserializerStream stream) {
        reset();
        this.stream = stream;
    }

    /** It updates the Peer to reflect that the Peer has just handshaked*/
    public void handshake() {
        this.connectionState = PeerConnectionState.HANDSHAKED;
    }

    /** It updates the Peer to reflect that the Peer has just disconnected */
    public void disconnect() {
        this.connectionState = PeerConnectionState.DISCONNECTED;
    }

    /** It updates the Peer to reflect that it's just arted to download this block */
    public void startDownloading(String blockHash) {
        reset();
        this.currentBlockInfo = new BlockProgressInfo(blockHash, this.peerAddress);
        this.workingState = PeerWorkingState.PROCESSING;
    }

    /**
     * It triggers and update of the BytesDownloaded and the BytesTotal values of this class, taking that
     * information from the underlying NIOInputStream that the DeserializerStream is connected to
     */
    public void updateBytesProgress() {
        // We only update the state if this Peer has actually started the downloading process...
        if (currentBlockInfo != null) {
            DeserializerStreamState streamState = stream.getState();
            // We only do the update if the current Msg being downloaded by this Peer is a BLOCK
            if (streamState.getCurrentHeaderMsg() != null
                    && streamState.getCurrentHeaderMsg().getCommand().equalsIgnoreCase(BlockMsg.MESSAGE_TYPE)) {

                // If the Deserializer stream is in CORRUPTED State (after throwing some error), we do nothing...
                if (!currentBlockInfo.isCorrupted()) {

                    if (streamState.getProcessState().equals(DeserializerStreamState.ProcessingBytesState.CORRUPTED)) {
                        currentBlockInfo.corrupted = true;
                        return;
                    }

                    Long bytesDownloaded = streamState.getCurrentMsgBytesReceived();
                    Long bytesTotal = streamState.getCurrentHeaderMsg() != null ? streamState.getCurrentHeaderMsg().getLength() : null;

                    // If these numbers are different from the previous already stored, then that means that this Peer is actually
                    // active, so we update the "lastBytesReceivedTimestamp" field..
                    if ((bytesDownloaded != this.currentBlockInfo.getBytesDownloaded()
                            || bytesTotal != this.currentBlockInfo.getBytesTotal())) {
                        this.currentBlockInfo.lastBytesReceivedTimestamp = Instant.now();
                    }

                    // We update the Speed (bytes/sec):
                    long totalSecs = Duration.between(this.currentBlockInfo.startTimestamp, Instant.now()).toSeconds();
                    if (totalSecs != 0)
                        this.downloadSpeed = (int) (bytesDownloaded / totalSecs);

                    this.currentBlockInfo.bytesTotal = bytesTotal;
                    this.currentBlockInfo.bytesDownloaded = bytesDownloaded;
                    this.currentBlockInfo.realTimeProcessing = streamState.getTreadState().dedicatedThreadRunning();

                }
            }
        }
    }


    // The following methods indicates if some threshodls have been broken...
    public boolean isIdleTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        if (currentBlockInfo.lastBytesReceivedTimestamp == null) return false;
        return (Duration.between(currentBlockInfo.lastBytesReceivedTimestamp,
                Instant.now()).compareTo(timeout) > 0);
    }

    public boolean isDownloadTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        return (Duration.between(currentBlockInfo.startTimestamp, Instant.now())
                .compareTo(timeout) > 0);
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(this.connectionState);
        result.append(", ");
        // we calculate the Progress status, if any:
        if (currentBlockInfo != null) {
            result.append(currentBlockInfo.toString());
            result.append(", ");
            if (currentBlockInfo.getRealTimeProcessing() != null)
                result.append(currentBlockInfo.getRealTimeProcessing() ? "Big block" : "small Block");
        }
        else result.append("Idle");
        return result.toString();
    }

}
