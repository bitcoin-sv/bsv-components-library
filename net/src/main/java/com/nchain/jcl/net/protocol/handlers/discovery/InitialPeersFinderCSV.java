package com.nchain.jcl.net.protocol.handlers.discovery;


import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.tools.files.FileUtils;
import com.nchain.jcl.tools.log.LoggerUtil;
import com.nchain.jcl.tools.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class returns an initial list of Peers to connect to after reading a static list of reliable Peers from a
 * CSV File.
 */
@Slf4j
public class InitialPeersFinderCSV implements InitialPeersFinder {
    private static final String CSV_SEPARATOR = ",";
    private static final String NET_FOLDER = "net";
    private static final String FILE_INITIAL_SUFFIX = "-discovery-handler-seed.csv";

    private FileUtils fileUtils;
    private DiscoveryHandlerConfig config;

    /** Constructor */
    public InitialPeersFinderCSV(FileUtils fileutils, DiscoveryHandlerConfig config) {
        this.fileUtils = fileutils;
        this.config = config;
    }

    @Override
    public List<PeerAddress> findPeers() {
        List<PeerAddress> result = new ArrayList<>();
        try {
            String fileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_INITIAL_SUFFIX;
            log.trace("loading Peer from CSV file: " + fileName + "...");
            Path dataPath = fileUtils.getRootPath();
            Path filePath = Paths.get(dataPath.toString(), NET_FOLDER, fileName);
            if (!Files.exists(filePath))
                return result;

            List<String> fileContent = Files.lines(filePath).collect(Collectors.toList());
            for (String line : fileContent) {
                try {
                    StringTokenizer tokens = new StringTokenizer(line, CSV_SEPARATOR);
                    String peerAddress = tokens.nextToken();
                    if (peerAddress.indexOf(":") == -1) {
                        String peerAddressStr = peerAddress + ":" + tokens.nextToken();
                        result.add(PeerAddress.fromIp(peerAddressStr));
                    } else {
                        result.add(PeerAddress.fromIp(peerAddress));
                    }
                } catch (UnknownHostException e) { continue;}
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
