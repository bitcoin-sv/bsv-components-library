package io.bitcoinsv.jcl.store.blockStore.metadata.provided;

import com.google.common.base.Objects;
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;


/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * This metadata stores information regarding a TxsStatus
 *
 * @author m.fletcher@nchain.com
 * @date 28/02/2022
 */
public class TxValidationMD implements Metadata {

    private boolean isValid;

    public TxValidationMD() {}
    public TxValidationMD(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    @Override
    public byte[] serialize() {
        // We use the Bitcoin codification to serialize it
        ByteArrayWriter writer = new ByteArrayWriter();
        writer.writeBoolean(isValid);
        return writer.reader().getFullContentAndClose();
    }

    @Override
    public void load(byte[] data) {
        if (data == null || data.length == 0) return;
        // We use the Bitcoin codification to deserialize it
        ByteArrayReader reader = new ByteArrayReader(data);
        this.isValid = reader.readBoolean();
        reader.closeAndClear();;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TxValidationMD other = (TxValidationMD) obj;
        return Objects.equal(this.isValid, other.isValid());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
