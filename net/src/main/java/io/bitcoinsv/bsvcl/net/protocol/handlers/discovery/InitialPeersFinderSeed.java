package io.bitcoinsv.bsvcl.net.protocol.handlers.discovery;


import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class returns an initial list of Peers to connect to after looking into a hardcoded list of
 * DNS, which is provided by the P2P Configuration
 */
public class InitialPeersFinderSeed implements InitialPeersFinder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(InitialPeersFinderSeed.class);
    private DiscoveryHandlerConfig config;

    /** Constructor */
    public InitialPeersFinderSeed(DiscoveryHandlerConfig protocolConfig) {
        this.config = protocolConfig;
    }

    @Override
    public List<PeerAddress> findPeers() {
        Set<PeerAddress> result = new HashSet<>();
        if (this.config.getDns() != null) {
            for (String dns : this.config.getDns()) {
                try {
                    InetAddress[] inetAddresses = InetAddress.getAllByName(dns);
                    for (InetAddress inetAddress : inetAddresses) {
                        PeerAddress peerAddress = PeerAddress.fromIp(inetAddress.getHostAddress() + ":" + config.getBasicConfig().getPort());
                        if (peerAddress != null) result.add(peerAddress);
                    }
                } catch (UnknownHostException e) {
                    continue;
                }
            }
        } else log.warn("No DNS defined for this Network Configuration!. No Network communication will be possible.");

        return result.stream().collect(Collectors.toList());
    }
}
