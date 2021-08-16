/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.handlers.handshake;

import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.events.control.PeerHandshakeRejectedEvent;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A placeholder for the variables needed by the Handshake Handler to keep track of each one of the Peers and their
 * handshake state.
 */
public class HandshakePeerInfo {
    // Peer Info:
    private PeerAddress peerAddress;

    // Version Messages exchanged:
    private VersionMsg versionMsgSent;
    private VersionMsg versionMsgReceived;

    // These vars store info about the status of the Handshake between the ProtocolHandler adn the Remote Peer above:
    private boolean ACKSent;
    private boolean ACKReceived;
    private boolean handshakeAccepted;
    private boolean handshakeRejected;

    // In case the handshake has been rejected, we store the info about it here:
    private PeerHandshakeRejectedEvent rejectedEvent;

    /** Constructor */
    public HandshakePeerInfo(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public synchronized void sendVersionMsg(VersionMsg versionMsgSent) {
        this.versionMsgSent = versionMsgSent;
    }

    public synchronized void receiveVersionMsg(VersionMsg versionMsgReceived) {
        this.versionMsgReceived = versionMsgReceived;
    }

    /** It registers that the Handshake has been accepted */
    public synchronized void acceptHandshake()  {
        this.handshakeAccepted = true;
        this.handshakeRejected = false;
    }

    /** It registers that the Handshake has been REJECTED */
    public synchronized void rejectHandshake()  {
        this.handshakeAccepted = false;
        this.handshakeRejected = true;
    }

    /** Return whether the Handshake has been performed successfully based on the values registered previously */
    public boolean checkHandshakeOK()                       { return ACKSent && ACKReceived; }

    public synchronized void sendACK()                      { this.ACKSent = true; }
    public synchronized void receiveACK()                   { this.ACKReceived = true; }
    public boolean isVersionMsgSent()                       { return versionMsgSent != null; }
    public boolean isVersionMsgReceived()                   { return versionMsgReceived != null; }

    public PeerAddress getPeerAddress()                     { return this.peerAddress; }
    public VersionMsg getVersionMsgSent()                   { return this.versionMsgSent; }
    public VersionMsg getVersionMsgReceived()               { return this.versionMsgReceived; }
    public boolean isACKSent()                              { return this.ACKSent; }
    public boolean isACKReceived()                          { return this.ACKReceived; }
    public boolean isHandshakeAccepted()                    { return this.handshakeAccepted; }
    public boolean isHandshakeRejected()                    { return this.handshakeRejected; }
    public PeerHandshakeRejectedEvent getRejectedEvent()    { return this.rejectedEvent; }
}
