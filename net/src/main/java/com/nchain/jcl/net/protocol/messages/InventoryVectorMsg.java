package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Inventory vectors are used for notifying other nodes about objects they have or data which is being requested.
 *
 */

public final class InventoryVectorMsg extends Message implements Serializable {

    public static final String MESSAGE_TYPE = "inventoryVec";

    // This value is the Length of the TYPE Field ONLY:
    public static final long VECTOR_TYPE_LENGTH = 4;

    public static enum VectorType {
        //	Any data of with this number may be ignored
        ERROR(0),

        // Hash is related to a transaction
        MSG_TX(1),

        // Hash is related to a data block
        MSG_BLOCK(2),

        //   Hash of a block header; identical to MSG_BLOCK. Only to be used in getdata message.
        //   Indicates the reply should be a merkleblock message rather than a block message;
        //   this only works if a bloom filter has been set.

        MSG_FILTERED_BLOCK(3),

         // Hash of a block header; identical to MSG_BLOCK. Only to be used in getdata message.
         // Indicates the reply should be a cmpctblock message. See BIP 152 for more info.

        MSG_CMPCT_BLOCK(4),

        OTHER( 5);

        int code;

        VectorType(int code) { this.code = code;}

        public int getValue() { return code;}

        public static VectorType fromCode(int code) {
            for (VectorType vectorType : VectorType.values())
                if (vectorType.code == code)
                    return vectorType;
            return OTHER;
        }
    }

    private final VectorType type;
    private final HashMsg hashMsg;

    protected InventoryVectorMsg( VectorType type, HashMsg hashMsg) {
        this.type = type;
        this.hashMsg = hashMsg;
        init();
    }

    @Override
    public String getMessageType()  { return MESSAGE_TYPE; }
    public VectorType getType()     { return this.type; }
    public HashMsg getHashMsg()     { return this.hashMsg; }

    @Override
    public String toString() {
        return "InventoryVectorMsg(type=" + this.getType() + ", hashMsg=" + this.getHashMsg() + ")";
    }

    @Override
    protected long calculateLength() {
        long length = VECTOR_TYPE_LENGTH + this.hashMsg.getLengthInBytes();;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public int hashCode() {
        return Objects.hashCode(type, hashMsg);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        InventoryVectorMsg other = (InventoryVectorMsg) obj;
        return Objects.equal(this.type, other.type)
                && Objects.equal(this.hashMsg, other.hashMsg);
    }

    public static InventoryVectorMsgBuilder builder() {
        return new InventoryVectorMsgBuilder();
    }


    public InventoryVectorMsgBuilder toBuilder() {
        return new InventoryVectorMsgBuilder()
                    .type(this.type)
                    .hashMsg(this.hashMsg);
    }

    /**
     * Builder
     */
    public static class InventoryVectorMsgBuilder {
        private VectorType type;
        private HashMsg hashMsg;

        public InventoryVectorMsgBuilder() {}

        public InventoryVectorMsg.InventoryVectorMsgBuilder type(VectorType type) {
            this.type = type;
            return this;
        }

        public InventoryVectorMsg.InventoryVectorMsgBuilder hashMsg(HashMsg hashMsg) {
            this.hashMsg = hashMsg;
            return this;
        }

        public InventoryVectorMsg build() {
            return new InventoryVectorMsg(type, hashMsg);
        }
    }
}
