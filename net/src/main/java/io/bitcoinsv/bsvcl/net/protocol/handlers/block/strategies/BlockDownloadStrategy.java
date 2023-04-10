package io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;

import java.util.List;
import java.util.Optional;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Strategy decides how the Peers are chosen for downloading a Block.
 */

public interface BlockDownloadStrategy {
     /**
      * It returns a Response to the Download-Request proposed, which is a combination of Peer+Block. It can return a
      * Reponse ASSIGNED (Which means the Peer can be assigned to that Block) or a Rejection.
      *
      * @param request             Combination of Block + Peer
      * @param availablePeers      List of all available Peer, NOT including the one in the Download Request
      * @param notAvailablePeers   List of all NoT available Peers (Peers connected bu busy at this very moment)
      * @return                    A DownloadResponse or EMPTY if its not possibel to provide an answer.
      */
     Optional<DownloadResponse> requestDownload(
             DownloadRequest request,
             List<PeerAddress> availablePeers,
             List<PeerAddress> notAvailablePeers);
}
