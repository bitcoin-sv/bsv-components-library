package com.nchain.jcl.net.protocol.handlers.message.streams.deserializer;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Runtime State of the Deserializer. It basically represents the state of the CACHE used when deserializing small
 * messages
 */
public class DeserializerState {
    private long numLoads = 0L;
    private long numHits = 0L;
    private double hitRatio = 0.0;

    DeserializerState(long numLoads, long numHits, double hitRatio) {
        this.numLoads = numLoads;
        this.numHits = numHits;
        this.hitRatio = hitRatio;
    }

    public static DeserializerStateBuilder builder()    { return new DeserializerStateBuilder(); }
    public long getNumLoads()                           { return this.numLoads; }
    public long getNumHits()                            { return this.numHits; }
    public double getHitRatio()                         { return this.hitRatio; }

    @Override
    public String toString() {
        return "DeserializerState(numLoads=" + this.getNumLoads() + ", numHits=" + this.getNumHits() + ", hitRatio=" + this.getHitRatio() + ")";
    }

    public DeserializerStateBuilder toBuilder() {
        return new DeserializerStateBuilder().numLoads(this.numLoads).numHits(this.numHits).hitRatio(this.hitRatio);
    }

    /**
     * Builder
     */
    public static class DeserializerStateBuilder {
        private long numLoads;
        private long numHits;
        private double hitRatio;

        DeserializerStateBuilder() {}

        public DeserializerState.DeserializerStateBuilder numLoads(long numLoads) {
            this.numLoads = numLoads;
            return this;
        }

        public DeserializerState.DeserializerStateBuilder numHits(long numHits) {
            this.numHits = numHits;
            return this;
        }

        public DeserializerState.DeserializerStateBuilder hitRatio(double hitRatio) {
            this.hitRatio = hitRatio;
            return this;
        }

        public DeserializerState build() {
            return new DeserializerState(numLoads, numHits, hitRatio);
        }
    }
}
