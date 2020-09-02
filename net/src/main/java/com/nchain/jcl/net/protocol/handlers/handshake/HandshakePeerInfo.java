package com.nchain.jcl.net.protocol.handlers.handshake;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.events.PeerHandshakeRejectedEvent;
import com.nchain.jcl.net.protocol.messages.VersionMsg;
import lombok.Getter;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-06 12:58
 *
 * A placeholder for the variables needed by the Handshake Handler to keep track of each one of the Peers and their
 * handshake state.
 */
public class HandshakePeerInfo {
    // Peer Info:
    @Getter private PeerAddress peerAddress;

    // Version Messages exchanged:
    @Getter private VersionMsg versionMsgSent;
    @Getter private VersionMsg versionMsgReceived;

    // These vars store info about the status of the Handshake between the ProtocolHandler adn the Remote Peer above:
    @Getter private boolean ACKSent;
    @Getter private boolean ACKReceived;
    @Getter private boolean handshakeAccepted;
    @Getter private boolean handshakeRejected;

    // In case the handshake has been rejected, we store the info about it here:
    @Getter private PeerHandshakeRejectedEvent rejectedEvent;

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

    public synchronized void sendACK() {
        this.ACKSent = true;
    }

    public synchronized void receiveACK() {
        this.ACKReceived = true;
    }

    public boolean isVersionMsgSent() {
        return versionMsgSent != null;
    }

    public boolean isVersionMsgReceived() {
        return versionMsgReceived != null;
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
    public boolean checkHandshakeOK() {
        return ACKSent && ACKReceived;
    }

}
