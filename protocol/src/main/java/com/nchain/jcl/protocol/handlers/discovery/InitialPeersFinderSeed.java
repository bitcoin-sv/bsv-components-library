package com.nchain.jcl.protocol.handlers.discovery;

import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.tools.files.FileUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-10 17:18
 *
 * This class returns an initial list of Peers to connect to after looking into a hardcoded list of
 * DNS, which is provided by the P2P Configuration
 */
public class InitialPeersFinderSeed implements InitialPeersFinder {

    private FileUtils fileUtils;
    private DiscoveryHandlerConfig config;

    /** Constructor */
    public InitialPeersFinderSeed(DiscoveryHandlerConfig protocolConfig) {
        this.config = protocolConfig;
    }

    @Override
    public List<PeerAddress> findPeers() {
        Set<PeerAddress> result = new HashSet<>();
        for (String dns : this.config.getDnsSeeds()) {
            try {
                InetAddress[] inetAddresses = InetAddress.getAllByName(dns);
                for (InetAddress inetAddress : inetAddresses) {
                    PeerAddress peerAddress = PeerAddress.fromIp(inetAddress.getHostAddress() + ":" + config.getBasicConfig().getPort());
                    if (peerAddress != null) result.add(peerAddress);
                }
            } catch (UnknownHostException e) { continue;}
        }
        return result.stream().collect(Collectors.toList());
    }
}
