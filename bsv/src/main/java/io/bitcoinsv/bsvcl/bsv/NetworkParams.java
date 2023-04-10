package io.bitcoinsv.bsvcl.bsv;

public abstract class NetworkParams {

    public final Network network;
    public final String[] dnsSeeds;
    public final int defaultPort;
    public final byte[] netMagic;

    protected NetworkParams(Network network, String[] dnsSeeds, int defaultPort, byte[] netMagic) {
        this.network = network;
        this.dnsSeeds = dnsSeeds;
        this.defaultPort = defaultPort;
        this.netMagic = netMagic;
    }

    public static NetworkParams getParams(Network network) {
        return switch (network) {
            case MAINNET -> new MainnetParams();
            case TESTNET -> new TestnetParams();
            case STN -> new StnParams();
            case REGTEST -> new RegtestParams();
        };
    }
}

class MainnetParams extends NetworkParams {
    MainnetParams() {
        super(Network.MAINNET,
                new String[] {"seed.bitcoinsv.io", "seed.satoshivision.network", "seed.bitcoinseed.directory"},
                8333,
                new byte[] {(byte) 0xe3, (byte) 0xe1, (byte) 0xf3, (byte) 0xe8});
    }
}

class TestnetParams extends NetworkParams {
    TestnetParams() {
        super(Network.TESTNET,
                new String[] {"testnet-seed.bitcoinsv.io", "testnet-seed.bitcoincloud.net", "testnet-seed.bitcoinseed.directory"},
                18333,
                new byte[] {(byte) 0xf4, (byte) 0xe5, (byte) 0xf3, (byte) 0xf4});
    }
}

class StnParams extends NetworkParams {
    StnParams() {
        super(Network.STN,
                new String[] {"stn-seed.bitcoinsv.io", "stn-seed.bitcoinseed.directory"},
                9333,
                new byte[] {(byte) 0xfb, (byte) 0xce, (byte) 0xc4, (byte) 0xf9});
    }
}

class RegtestParams extends NetworkParams {
    RegtestParams() {
        super(Network.MAINNET,
                new String[] {},
                18444,
                new byte[] {(byte) 0xda, (byte) 0xb5, (byte) 0xbf, (byte) 0xfa});
    }
}
