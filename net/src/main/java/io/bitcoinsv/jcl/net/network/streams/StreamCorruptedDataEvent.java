package io.bitcoinsv.jcl.net.network.streams;

import io.bitcoinsv.jcl.net.network.PeerAddress;

/**
 * This event represents corrupted data case/response when transformation of the {@link StreamDataEvent} resulted in an error.
 * Preservers peer information, message command and message bytes.
 */
public class StreamCorruptedDataEvent extends StreamEvent {
    private final PeerAddress peerAddress;
    private final String messageCommand;
    private final byte[] messageBytes;

    public StreamCorruptedDataEvent(PeerAddress peerAddress, String messageCommand, byte[] messageBytes) {
        this.peerAddress = peerAddress;
        this.messageCommand = messageCommand;
        this.messageBytes = messageBytes;
    }

    public PeerAddress getPeerAddress() {
        return this.peerAddress;
    }

    public String getMessageCommand() {
        return this.messageCommand;
    }

    public byte[] getMessageBytes() {
        return this.messageBytes;
    }
}
