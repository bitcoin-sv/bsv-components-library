package com.nchain.jcl.store.keyValue.common;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An object that stores a List of Hashes. It might used for storing Block Hashes, Tx Hashes, etc.
 */
@Builder(toBuilder = true)
@Value
public class HashesList implements Serializable {
    @Builder.Default
    private List<String> hashes = new ArrayList<>();

    public void addHash(String hash)        { if (!hashes.contains(hash)) hashes.add(hash); }
    public void removeHash(String hash)     { hashes.remove(hash); }
    public void clear()                     { hashes.clear(); }

    @Override
    public String toString()                { return hashes.stream().collect(Collectors.joining(","));}
}
