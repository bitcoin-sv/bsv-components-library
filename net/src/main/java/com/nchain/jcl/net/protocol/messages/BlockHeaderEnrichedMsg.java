package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Enriched block headers is embedded in headersen message. This message returns same data as BlockHeader message with
 * the addition of fields for actual number of transactions that are included in the block and proof of inclusion
 * for coinbase transaction along with the whole coinbase transaction.
 */
@Value
public class BlockHeaderEnrichedMsg extends Message {
    public static final String MESSAGE_TYPE = "blockHeaderEnriched";
    public static final int TIMESTAMP_LENGTH = 4;
    public static final int NBITS_LENGTH = 4;
    public static final int NONCE_LENGTH = 4;
    public static final int NO_MORE_HEAD_LENGTH = 1;
    public static final int HAS_COINBASEDATA_LENGTH = 1;
    public static final int TX_CNT = 8;

    private final long version;
    private final HashMsg prevBlockHash;
    private final HashMsg merkleRoot;
    private final long creationTimestamp;
    private final long nBits;
    private final long nonce;
    private final long transactionCount;
    private final boolean noMoreHeaders;
    private final boolean hasCoinbaseData;
    private final List<HashMsg> coinbaseMerkleProof;
    private final VarStrMsg coinbase;

    //coinbaseTX is not part of the BlockHeaderEnrichedMsg itself
    private final Tx coinbaseTX;



    @Builder
    public BlockHeaderEnrichedMsg(long version, HashMsg prevBlockHash, HashMsg merkleRoot, long creationTimestamp,
                                  long nBits, long nonce, long transactionCount, boolean noMoreHeaders,
                                  boolean hasCoinbaseData,List<HashMsg> coinbaseMerkleProof, VarStrMsg coinbase,Tx coinbaseTX) {
        this.version = version;
        this.prevBlockHash = prevBlockHash;
        this.merkleRoot = merkleRoot;
        this.creationTimestamp = creationTimestamp;
        this.nBits = nBits;
        this.nonce = nonce;
        this.transactionCount = transactionCount;
        this.noMoreHeaders = noMoreHeaders;
        this.hasCoinbaseData = hasCoinbaseData;
        this.coinbaseMerkleProof = coinbaseMerkleProof.stream().collect(Collectors.toUnmodifiableList());
        this.coinbase = coinbase;
        this.coinbaseTX = coinbaseTX;
        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected long calculateLength() {
            long length = 4 + prevBlockHash.getLengthInBytes() + merkleRoot.getLengthInBytes()
                    + TIMESTAMP_LENGTH + NBITS_LENGTH + NONCE_LENGTH + TX_CNT
                    + NO_MORE_HEAD_LENGTH + HAS_COINBASEDATA_LENGTH;

            if(hasCoinbaseData) {
                int size = coinbaseMerkleProof.size();
                length = length + size *  HashMsg.HASH_LENGTH + coinbase.getLengthInBytes();
             }

        return length;
    }

    @Override
    protected void validateMessage() {
    }
}
