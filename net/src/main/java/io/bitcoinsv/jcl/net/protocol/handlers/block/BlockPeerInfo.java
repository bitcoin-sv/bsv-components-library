package io.bitcoinsv.jcl.net.protocol.handlers.block;

import com.google.common.base.Strings;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerStreamState;
import io.bitcoinsv.jcl.tools.util.StringUtils;

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
        DISCARDED,
        IN_LIMBO;
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

    // Info about the Block being currently downloaded by this Peer:
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
    public void setDownloadSpeed(Integer speed)     { this.downloadSpeed = speed; }
    public DeserializerStream getStream()           { return this.stream; }
    public BlockProgressInfo getCurrentBlockInfo()  { return this.currentBlockInfo; }

    public boolean isConnected()                    { return this.connectionState.equals(PeerConnectionState.CONNECTED);}
    public boolean isHandshaked()                   { return this.connectionState.equals(PeerConnectionState.HANDSHAKED);}
    public boolean isDisconnected()                 { return this.connectionState.equals(PeerConnectionState.DISCONNECTED);}

    public boolean isIdle()                         { return this.workingState.equals(PeerWorkingState.IDLE);}
    public boolean isProcessing()                   { return this.workingState.equals(PeerWorkingState.PROCESSING);}
    public boolean isDiscarded()                    { return this.workingState.equals(PeerWorkingState.DISCARDED);}
    public boolean isInLimbo()                      { return this.workingState.equals(PeerWorkingState.IN_LIMBO);}

    public boolean isDownloading(String blockHash)  { return ((this.currentBlockInfo != null) && (this.currentBlockInfo.getHash().equalsIgnoreCase(blockHash)));}

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
     * It set this Peer to ION_LIMBO State, meaning this Peer is in an unpredictable state: It's supposed to be
     * downloading a Block, but it also seems to have an issue, so we are not getting more info from it. But we might
     * still get it in the near future. So this peer will remain in this state until:
     *  - we eventually receive the remaining parts of the block, which will finish the download
     *  - a timeout is triggered and the download will be processed as a fail,iure, so the block wuill be re-attempted
     *    with another Peer
     */
    protected void setToLimbo() {
        this.workingState = PeerWorkingState.IN_LIMBO;
    }

    protected void setToProcessing() {
        this.workingState = PeerWorkingState.PROCESSING;
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

        // IMPORTANT: When a Peer gets disconnected, we need to remove the reference to its Stream here, otherwise
        // the memory used by the internal ByteArrayBuffer used in that Stream will not be GC-collected, leading
        // to a Memory Leak and a OutOfMemory Error:

        // we remove the ref, making it eligible for GC:
        this.stream = null;
    }

    /** It updates the Peer to reflect that it's just started to download this block */
    protected void startDownloading(String blockHash, int numAttempt) {
        reset();
        setToProcessing();
        this.currentBlockInfo = new BlockProgressInfo(blockHash, this.peerAddress, numAttempt);
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
            if (currentHeaderMsg != null && currentHeaderMsg.getMsgCommand().equalsIgnoreCase(BlockMsg.MESSAGE_TYPE)) {

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
                    currentBlockInfo.updateBytesTotal(currentHeaderMsg.getMsgLength());
                }

                // If the Deserializer stream is in CORRUPTED State (after throwing some error), we do nothing...
                if (!currentBlockInfo.isCorrupted()) {

                    if (streamState.getProcessState().equals(DeserializerStreamState.ProcessingBytesState.CORRUPTED)) {
                        currentBlockInfo.setCorrupted();
                        return;
                    }

                    // We update the bytes that have been downloaded, and the total Bytes:
                    Long bytesDownloaded = streamState.getCurrentMsgBytesReceived();

                    // If these numbers are different from the previous already stored, then that means that this Peer is actually
                    // active, so we update the "lastBytesReceivedTimestamp" field..
                    if (!bytesDownloaded.equals(this.currentBlockInfo.getBytesDownloaded())) {
                        this.currentBlockInfo.updateLastBytesReceivedTimestamp(Instant.now());
                    }

                    // We update the Speed (bytes/sec):
                    long totalSecs = Duration.between(this.currentBlockInfo.getStartTimestamp(), Instant.now()).toSeconds();
                    if (totalSecs != 0)
                        this.downloadSpeed = (int) (bytesDownloaded / totalSecs);

                    this.currentBlockInfo.updateBytesDownloaded(bytesDownloaded);
                    this.currentBlockInfo.setRealTimeProcessing(streamState.getTreadState().dedicatedThreadRunning());
                }
            }
        }
    }

    // Indicates if this Peer has broken the IDLE time-limit, meaning he has NOT sent any bytes at all during
    // that time:
    protected boolean isIdleTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        if (currentBlockInfo.getLastBytesReceivedTimestamp() == null) return false;
        return (Duration.between(currentBlockInfo.getLastBytesReceivedTimestamp(),
                Instant.now()).compareTo(timeout) > 0);
    }

    // Indicates if this Peer is taking longer than we allow it to download a Block
    protected boolean isDownloadTimeoutBroken(Duration timeout) {
        if (currentBlockInfo == null) return false;
        return (Duration.between(currentBlockInfo.getStartTimestamp(), Instant.now())
                .compareTo(timeout) > 0);
    }

    // Indicates if this Peer is too slow. A Peer is considered "too slow" if:
    // - It already downloaded a minimum number of bytes AND
    // - Its downloading a Block AND
    // - the avg Speed (bytes/sec) is lower than the minSpeed given as parameter
    protected boolean isTooSlow(int minBytesPerSec) {
        if (minBytesPerSec <= 0) return false;
        if (currentBlockInfo == null) return false;

        final int MIN_BYTES_READ    = 10_000; // minimum size: 10K
        HeaderMsg currentHeaderMsg  = stream.getState().getCurrentHeaderMsg();
        boolean isBlock             = currentHeaderMsg.getMsgCommand().equalsIgnoreCase(BlockMsg.MESSAGE_TYPE);
        long numBytesSoFar          = stream.getState().getCurrentMsgBytesReceived();

        if (currentHeaderMsg != null && isBlock && (numBytesSoFar >= MIN_BYTES_READ)) {
            long numMillisSoFar = Duration.between(currentBlockInfo.getStartTimestamp(),Instant.now()).toMillis();
            long currentSpeed   = (numBytesSoFar / numMillisSoFar) * 1000;
            return (currentSpeed < minBytesPerSec);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(StringUtils.fixedLength(this.peerAddress.toString(), 36));
        result.append(" ");

        // If this Peer is downloading a Block, we add more detail:
        BlockProgressInfo blockProgressInfo = currentBlockInfo;
        if (blockProgressInfo != null) {
            // Downloading time...
            Duration downloadingTime = Duration.between(blockProgressInfo.getStartTimestamp(), Instant.now());
            result.append(StringUtils.fixedLength("[" + downloadingTime.toSeconds() + " secs]",10));
            result.append(" ");

            // Block Details:
            result.append(blockProgressInfo);
            result.append(" ");

            // Download Speed:
            if (blockProgressInfo.getBytesDownloaded()!= null && blockProgressInfo.getBytesTotal() != null) {
                DecimalFormat speedFormat = new DecimalFormat("#0.0");
                Integer peerSpeed = getDownloadSpeed();
                String speedStr = (peerSpeed != null)
                        ? (peerSpeed != Integer.MAX_VALUE)
                        ? speedFormat.format((double) peerSpeed / 1_000)
                        : "¿?"
                        : "0.0";
                result.append(StringUtils.fixedLength(speedStr + " KB/sec", 15));
            }
        } else {
            result.append(this.connectionState).append("-");
            result.append(this.workingState);
        }

        return result.toString();
    }

}