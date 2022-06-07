package io.bitcoinsv.jcl.net.protocol.messages.merkle;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;

/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * @author m.fletcher@nchain.com
 * @date 13/09/2021#
 *
 * The MerkleNodeMsg contains a VarInt type field along with the 32 bytes for the hash, as defined in the spec: https://tsc.bitcoinassociation.net/standards/merkle-proof-standardised-format/
 */
public class MerkleNode {
    private VarIntMsg type;
    private HashMsg hash;

    public MerkleNode(VarIntMsg type, HashMsg hash){
        this.type = type;
        this.hash = hash;
    }

    public HashMsg getHash() {
        return hash;
    }

    public void setHash(HashMsg hash) {
        this.hash = hash;
    }

    public VarIntMsg getType() {
        return type;
    }

    public void setType(VarIntMsg type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        MerkleNode other = (MerkleNode) obj;
        return Objects.equal(this.type, other.type)
                && Objects.equal(this.hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, hash);
    }
}


