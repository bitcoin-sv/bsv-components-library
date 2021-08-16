package io.bitcoinsv.jcl.net.protocol.handlers.block;

import com.google.common.base.Strings;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.streams.deserializer.DeserializerStream;
import io.bitcoinsv.jcl.net.protocol.streams.deserializer.DeserializerStreamState;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores all the info the blockDownloader Handler needs to keep track of for each Peer, in order
 * to manage the download of blocks.
 */
public class BlockPeerInfo {

    /**
     * Definition of the different States a Peer is regarding its connection. When a Peer is disconnected, its
     * info is NOT removed, instead its kept so we can "remember" some parameters about it like the download speed.
     * So we keep information about Peers even though they are disconnected, and we need this state to differentiate
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
        PROCESSING,
        DISCARDED;
    }

    /**
     * Definition of Info about the Current Block being downloaded by this Peer.
     */
    public class BlockProgressInfo {
        protected String hash;
        protected int numAttempt;
        protected BlockHeaderMsg blockHeaderMsg;
        protected PeerAddress peerAddress; // a bit redundant, but it's ok
        protected boolean corrupted;
        protected Long bytesTotal;
        protected Long bytesDownloaded;
        protected Boolean realTimeProcessing;
        protected Instant startTimestamp;
        protected Instant lastBytesReceivedTimestamp;

        public BlockProgressInfo(String hash, PeerAddress peerAddress, int numAttempt) {
            this.hash = hash;
            this.numAttempt = numAttempt;
            this.peerAddress = peerAddress;
            this.startTimestamp = Instant.now();
            this.lastBytesReceivedTimestamp = Instant.now();
        }

        @Override
        public String toString() {
            StringBuffer result = new StringBuffer();
            result.append(hash).append(" : ");
            result.append(Strings.padEnd("#" + numAttempt,3,' ')).append(" : ");
            if (corrupted) result.append("CORRUPTED");
            else {
                DecimalFormat format = new DecimalFormat("#0.0");
                if (bytesDownloaded != null && bytesTotal != null) {
                    String bytesTotalStr = format.format((double) bytesTotal / 1_000_000) + " MB";
                    String bytesDownStr =  format.format((double) bytesDownloaded / 1_000_000) + " MB";
                    String progressStr = (getProgressPercentage() == null)? "¿? %" : (getProgressPercentage() + " %");
                    result.append(Strings.padEnd(progressStr, 4, ' '));
                    String bytesRead = " [" + bytesDownStr + " / " + bytesTotalStr + "]";
                    result.append(Strings.padEnd(bytesRead, 22, ' '));
                }
            }
            return result.toString();
        }

        public String getHash()                         { return this.hash; }
        public BlockHeaderMsg getBlockHeaderMsg()       { return this.blockHeaderMsg; }
        public PeerAddress getPeerAddress()             { return this.peerAddress; }
        public boolean isCorrupted()                    { return this.corrupted; }
        public Long getBytesTotal()                     { return this.bytesTotal; }
        public Long getBytesDownloaded()                { return this.bytesDownloaded; }
        public Boolean getRealTimeProcessing()          { return this.realTimeProcessing; }
        public Instant getStartTimestamp()              { return this.startTimestamp; }
        public Instant getLastBytesReceivedTimestamp()  { return this.lastBytesReceivedTimestamp; }

        public Integer getProgressPercentage() {
            Integer result = (bytesTotal == null) ? null : (int)  (bytesDownloaded * 100 / bytesTotal);
            return result;
        }

    }

    // A comparator that orders the Peers by Speed (high speed first)
    public static final Comparator<BlockPeerInfo> SPEED_COMPARATOR = (peerA, peerB) -> peerB.downloadSpeed - peerA.downloadSpeed;

    // Peer Info:
    private PeerAddress peerAddress;
    private PeerConnectionState connectionState;
    private PeerWorkingState workingState;
    private Integer downloadSpeed; // bytes/sec

    // A reference to the Deserializer Stream used by this Peer:
    private DeserializerStream stream;

    // Info bout the Block being currently downloaded by this Peer:
    private BlockProgressInfo currentBlockInfo;


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

    public PeerAddress getPeerAddress()             { return this.peerAddress; }
    public PeerConnectionState getConnectionState() { return this.connectionState; }
    public PeerWorkingState getWorkingState()       { return this.workingState; }
    public Integer getDownloadSpeed()               { return this.downloadSpeed; }
    public DeserializerStream getStream()           { return this.stream; }
    public BlockProgressInfo getCurrentBlockInfo()  { return this.currentBlockInfo; }

    public boolean isConnected()                    { return this.connectionState.equals(PeerConnectionState.CONNECTED);}
    public boolean isHandshaked()                   { return this.connectionState.equals(PeerConnectionState.HANDSHAKED);}
    public boolean isDisconnected()                 { return this.connectionState.equals(PeerConnectionState.DISCONNECTED);}

    public boolean isIdle()                         { return this.workingState.equals(PeerWorkingState.IDLE);}
    public boolean isProcessing()                   { return this.workingState.equals(PeerWorkingState.PROCESSING);}
    public boolean isDiscarded()                    { return this.workingState.equals(PeerWorkingState.DISCARDED);}

    /**
     * It resets the peer, to make it ready to download a new Block. This Peer might have been used to downloadad
     * another Block previously, so every time we reset it, we reset the properties related to the current
     * download, but some other "global" variabels are kept, like the download Speed
     */
    protected void reset() {
        this.workingState = PeerWorkingState.IDLE;
        this.currentBlockInfo = null;
    }

    /**
     * It discards this Peer, prabably due to a previous error while downloading a Block from it
     */
    protected void discard() {
        this.workingState = PeerWorkingState.DISCARDED;
        this.currentBlockInfo = null;
    }

    /**
     * It sets this peer to IDLE State
     */
    protected void setIdle() {
        this.workingState = PeerWorkingState.IDLE;
        this.currentBlockInfo = null;
    }

    /** It updates the Peer to reflect that it's just connected */
    protected void connect(DeserializerStream stream) {
        reset();
        this.stream = stream;
    }

    /** It updates the Peer to reflect that the Peer has just handshaked*/
    protected void handshake() {
        reset();
        this.connectionState = PeerConnectionState.HANDSHAKED;
    }

    /** It updates the Peer to reflect that the Peer has just disconnected */
    protected void disconnect() {
        reset();
        this.connectionState = PeerConnectionState.DISCONNECTED;
    }

    /** It updates the Peer to reflect that it's just started to download this block */
    protected void startDownloading(String blockHash, int numAttempt) {
        reset();
        this.currentBlockInfo = new BlockProgressInfo(blockHash, this.peerAddress, numAttempt);
        this.workingState = PeerWorkingState.PROCESSING;
    }

    /**
     * It triggers and update the value of the BytesDownloaded and the BytesTotal values of this class, taking that
     * information from the underlying NIOInputStream that the DeserializerStream is connected to
     */
    protected synchronized void updateBytesProgress() {
        // We only update the state if this Peer has actually started the downloading process...
        if (currentBlockInfo != null) {
            DeserializerStreamState streamState = stream.getState();
            HeaderMsg currentHeaderMsg = streamState.getCurrentHeaderMsg();
            // We only do the update if the current Msg being downloaded by this Peer is a BLOCK
            if (currentHeaderMsg != null && currentHeaderMsg.getCommand().equalsIgnoreCase(BlockMsg.MESSAGE_TYPE)) {

                // We set the Total Bytes. This is a bit tricky:
                // When a Peer starts the downloading of a block, "bytesTotal" is reset to ZERO. then, and while
                // the peer is downloading the block, this method is called on a frequency basis in order to update
                // the downloading State. At that moment, we update "bytesTotal" with the new value that is stored in
                // the STATE of the Stream connected to this Peer. But the Stream might not have started downloading the
                // new Block yet, so the Header stored in its state is still referencing the old Block. So we only
                // update "bytesTotal" if the Stream is SEEKING a BODY, which means that the Stream has already parsed
                // the new Header...

               if (stream.getState().getProcessState() == DeserializerStreamState.ProcessingBytesState.SEEIKING_BODY ||
                    stream.getState().getProcessState() == DeserializerStreamState.ProcessingBytesState.DESERIALIZING_BODY) {
                    currentBlockInfo.bytesTotal = currentHeaderMsg.getLength();
                }

                // If the Deserializer stream is in CORRUPTED State (after throwing some error), we do nothing...
                if (!currentBlockInfo.isCorrupted()) {

                    if (streamState.getProcessState().equals(DeserializerStreamState.ProcessingBytesState.CORRUPTED)) {
                        currentBlockInfo.corrupted = true;
                        return;
                    }

                    // We update the bytes that have been downloaded, and the total Bytes:
                    Long bytesDownloaded = streamState.getCurrentMsgBytesReceived();

                    // If these numbers are different from the previous already stored, then that means that this Peer is actually
                    // active, so we update the "lastBytesReceivedTimestamp" field..
                    if (bytesDownloaded != this.currentBlockInfo.getBytesDownloaded()) {
                        this.currentBlockInfo.lastBytesReceivedTimestamp = Instant.now();
                    }

                    // We update the Speed (bytes/sec):
                    long totalSecs = Duration.between(this.currentBlockInfo.startTimestamp, Instant.now()).toSeconds();
                    if (totalSecs != 0)
                        this.downloadSpeed = (int) (bytesDownloaded / totalSecs);

                    this.currentBlockInfo.bytesDownloaded = bytesDownloaded;
                    this.currentBlockInfo.realTimeProcessing = streamState.getTreadState().dedicatedThreadRunning();
                }
            }
        }
    }


    // The following methods indicates if some thresholds have been broken...
    protected boolean isIdleTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        if (currentBlockInfo.lastBytesReceivedTimestamp == null) return false;
        return (Duration.between(currentBlockInfo.lastBytesReceivedTimestamp,
                Instant.now()).compareTo(timeout) > 0);
    }

    protected boolean isDownloadTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        return (Duration.between(currentBlockInfo.startTimestamp, Instant.now())
                .compareTo(timeout) > 0);
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(Strings.padEnd(this.peerAddress.toString(), 36, ' ')).append(" : ");

        // we calculate the Progress status, if any:
        BlockProgressInfo blockProgressInfo = currentBlockInfo;
        if (blockProgressInfo != null) {
            result.append(blockProgressInfo.toString());

            //if (blockProgressInfo.getRealTimeProcessing() != null)
            //    result.append(blockProgressInfo.getRealTimeProcessing() ? "[Big block]" : "").append(" : ");

            // We print this Peer download Speed:
            DecimalFormat speedFormat = new DecimalFormat("#0.0");
            Integer peerSpeed = getDownloadSpeed();
            if (peerSpeed != null && blockProgressInfo.bytesDownloaded != null && peerSpeed != Integer.MAX_VALUE) {
                String speedStr = speedFormat.format((double) peerSpeed / 1_000);
                result.append(Strings.padEnd("[ " + speedStr + " KB/sec ]",17, ' '));
                result.append(" : ");
            }

            // We print the downloading time:
            Duration downloadingTime = Duration.between(blockProgressInfo.startTimestamp, Instant.now());
            if (downloadingTime.toSeconds() > 0) {
                result.append("[" + downloadingTime.toSeconds() + " secs]");
            }
        } else {
            result.append(this.connectionState).append("-");
            result.append(this.workingState);
        }

        return result.toString();
    }

}
