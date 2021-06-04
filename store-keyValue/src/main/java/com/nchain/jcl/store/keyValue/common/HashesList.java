package com.nchain.jcl.store.keyValue.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An object that stores a List of Hashes. It might used for storing Block Hashes, Tx Hashes, etc.
 */
public final class HashesList implements Serializable {
    private List<String> hashes = new ArrayList<>();

    HashesList(List<String> hashes) {
        if (hashes != null) this.hashes = hashes;
    }

    public void addHash(String hash)            { if (!hashes.contains(hash)) hashes.add(hash); }
    public void removeHash(String hash)         { hashes.remove(hash); }
    public void clear()                         { hashes.clear(); }

    @Override
    public String toString()                    { return hashes.stream().collect(Collectors.joining(","));}
    public List<String> getHashes()             { return this.hashes; }
    public HashesListBuilder toBuilder()        { return new HashesListBuilder().hashes(this.hashes); }
    public static HashesListBuilder builder()   { return new HashesListBuilder(); }

    /**
     * Builder
     */
    public static class HashesListBuilder {
        private List<String> hashes;

        public HashesListBuilder() {
        }

        public HashesList.HashesListBuilder hashes(List<String> hashes) {
            this.hashes = hashes;
            return this;
        }
        public HashesList.HashesListBuilder hash(String hash) {
            this.hashes =new ArrayList<>();
            this.hashes.add(hash);
            return this;
        }

        public HashesList build() {
            return new HashesList(hashes);
        }
    }
}
