package io.bitcoinsv.bsvcl.net.protocol.handlers.block;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.bsvcl.tools.util.StringUtils;

import java.text.DecimalFormat;
import java.time.Instant;

/**
 * This class tracks the Progress of Downloading a Block from a Peer
 */
public class BlockProgressInfo {
    private String hash;
    private int numAttempt;
    private BlockHeaderMsg blockHeaderMsg;
    private PeerAddress peerAddress; // a bit redundant, but it's ok
    private boolean corrupted;
    private Long bytesTotal;
    private long bytesDownloaded;
    private Boolean realTimeProcessing;
    private Instant startTimestamp;
    private Instant lastBytesReceivedTimestamp;

    /**
     * Constructor
     */
    public BlockProgressInfo(String hash, PeerAddress peerAddress, int numAttempt) {
        this.hash = hash;
        this.numAttempt = numAttempt;
        this.peerAddress = peerAddress;
        this.startTimestamp = Instant.now();
        this.lastBytesReceivedTimestamp = Instant.now();
    }

    // Getters:

    public String getHash()                         { return hash;}
    public int getNumAttempt()                      { return numAttempt;}
    public BlockHeaderMsg getBlockHeaderMsg()       { return blockHeaderMsg;}
    public PeerAddress getPeerAddress()             { return peerAddress;}
    public boolean isCorrupted()                    { return corrupted;}
    public Long getBytesTotal()                     { return bytesTotal;}
    public Long getBytesDownloaded()                { return bytesDownloaded;}
    public Boolean getRealTimeProcessing()          { return realTimeProcessing;}
    public Instant getStartTimestamp()              { return startTimestamp;}
    public Instant getLastBytesReceivedTimestamp()  { return lastBytesReceivedTimestamp;}

    public Integer getProgressPercentage() {
        Integer result = (bytesTotal == null) ? null : (int)  (bytesDownloaded * 100 / bytesTotal);
        return result;
    }

    // Setter/Updates

    public void updateLastBytesReceivedTimestamp(Instant now) {
        this.lastBytesReceivedTimestamp = now;
    }

    public void updateBytesTotal(long bytesTotal) {
        this.bytesTotal = bytesTotal;
    }

    public void updateBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

    public void setCorrupted() {
        this.corrupted = true;
    }

    public void setRealTimeProcessing(boolean realTimeProcessing) {
        this.realTimeProcessing = realTimeProcessing;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        // Block being downloaded:
        result.append(hash);
        result.append(" ");

        // Number of Attempts of downloading this Block:
        result.append(StringUtils.fixedLength("#" + numAttempt, 4));
        result.append(" ");

        // If state is CORRUPTED, we do not log anything else:
        if (corrupted) {
            result.append("CORRUPTED !!");
        } else {
            // Download Speed:
            // We only log it if we have already downloading some of the block otherwise we dont even know its size:
            if ((bytesDownloaded > 0) && bytesTotal != null) {
                // Progress (percentage):
                String progressStr = (getProgressPercentage() == null)? "¿? %" : (getProgressPercentage() + " %");
                result.append(StringUtils.fixedLength(progressStr, 5));
                result.append(" ");

                // Bytes downloaded / total:
                DecimalFormat format = new DecimalFormat("#0.0");
                String bytesTotalStr = format.format((double) bytesTotal / 1_000_000);
                String bytesDownStr =  format.format((double) bytesDownloaded / 1_000_000);
                String bytesStr = StringUtils.fixedLength(bytesDownStr + "/" + bytesTotalStr + " MB", 15);
                result.append(bytesStr);

            } else {
                result.append("nothing received yet...");
            }
        }
        return result.toString();
    }

}