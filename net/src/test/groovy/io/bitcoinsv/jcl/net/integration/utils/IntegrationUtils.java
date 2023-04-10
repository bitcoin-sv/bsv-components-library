package io.bitcoinsv.jcl.net.integration.utils;

import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig;

/**
 * Utility class for Integration Tests
 */
public class IntegrationUtils {

    /**
     * Returns a DiscoveryHandler Config with a hardcoded list of IP addresses to connect to.
     * If DNSs of MAinNET do not work properly, at lest we'll be able to connect to some of these IPs.
     * The list used here has been obtained from WhatsOnChain, so this is not a permanent solution.
     * @param currentConfig current DiscoveryHandlerConfig we want to extend
     * @return  current Config where IPs have been added
     */
    public static DiscoveryHandlerConfig getDiscoveryHandlerConfigMainnet(DiscoveryHandlerConfig currentConfig) {
        DiscoveryHandlerConfig updatedConfig = currentConfig.toBuilder()
                .addInitialConnection("144.76.117.158:8333")
                .addInitialConnection("65.108.132.250:8333")
                .addInitialConnection("23.250.18.170:8333")
                .addInitialConnection("3.69.24.55:8333")
                .addInitialConnection("95.216.243.249:8333")
                .addInitialConnection("3.120.175.133:8333")
                .addInitialConnection("139.59.35.196:8333")
                .addInitialConnection("95.217.197.54:8333")
                .build()   ;
        return updatedConfig;
    }

}
