package io.bitcoinsv.bsvcl.bsv;

public enum Network {
    MAINNET("mainnet"),
    TESTNET("testnet"),
    REGTEST("regtest"),
    STN("stn");

    private final String id;

    Network(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}


