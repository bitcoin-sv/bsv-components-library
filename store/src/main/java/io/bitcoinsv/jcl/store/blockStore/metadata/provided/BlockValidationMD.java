package io.bitcoinsv.jcl.store.blockStore.metadata.provided;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 27/04/2021
 *
 * This metadata stores information about a Block that helps identify whether the Block has been already downloaded
 * completely (since the Block download is usually performed in batches) and validated
 */
public class BlockValidationMD implements Metadata {
    // Official total num of Txs (unlike the value returned by the method "getBlockNumTxs", which returns the current
    // number of Txs stored in the DB, which might be lower if wer are still downloading the Block)
    private long numTxs;

    private boolean downloaded;
    private boolean validated;

    /** Constructor */
    public BlockValidationMD() {
        this(0, false, false);
    }

    /** Constructor */
    public BlockValidationMD(long numTxs, boolean downloaded, boolean validated) {
        this.numTxs = numTxs;
        this.downloaded = downloaded;
        this.validated = validated;
    }

    public long getNumTxs()                         { return this.numTxs; }
    public boolean isDownloaded()                   { return this.downloaded;}
    public boolean isValidated()                    { return this.validated;}

    public void setNumTxs(long numTxs)              { this.numTxs = numTxs; }
    public void setDownloaded(boolean downloaded)   { this.downloaded = downloaded; }
    public void setValidated(boolean validated)     { this.validated = validated; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BlockValidationMD other = (BlockValidationMD) obj;
        return Objects.equal(this.numTxs, other.numTxs)
                && Objects.equal(this.downloaded, other.downloaded)
                && Objects.equal(this.validated, other.validated);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public byte[] serialize() {
        // We use the Bitcoin codification to serialize it
        ByteArrayWriter writer = new ByteArrayWriter();
        writer.writeUint64LE(numTxs);
        writer.writeBoolean(downloaded);
        writer.writeBoolean(validated);
        return writer.reader().getFullContentAndClose();
    }

    @Override
    public void load(byte[] data) {
        if (data == null || data.length == 0) return;
        // We use the Bitcoin codification to deserialize it
        ByteArrayReader reader = new ByteArrayReader(data);
        this.numTxs = reader.readInt64LE();
        this.downloaded = reader.readBoolean();
        this.validated = reader.readBoolean();
        reader.closeAndClear();;
    }
}
