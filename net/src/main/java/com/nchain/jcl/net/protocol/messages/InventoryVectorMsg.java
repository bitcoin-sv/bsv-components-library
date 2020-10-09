package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Inventory vectors are used for notifying other nodes about objects they have or data which is being requested.
 *
 */

@Value
@EqualsAndHashCode
public class InventoryVectorMsg extends Message {

    public static final String MESSAGE_TYPE = "inventoryVec";

    // This value is the Length of the TYPE Field ONLY:
    public static long VECTOR_TYPE_LENGTH = 4;

    public static enum VectorType {
        ERROR(0), //	Any data of with this number may be ignored
        MSG_TX(1), // Hash is related to a transaction
        MSG_BLOCK(2),//Hash is related to a data block

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

    private VectorType type;
    private HashMsg hashMsg;

    @Builder
    protected InventoryVectorMsg( VectorType type, HashMsg hashMsg) {
        this.type = type;
        this.hashMsg = hashMsg;

        init();
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }


    @Override
    protected long calculateLength() {
        long length = VECTOR_TYPE_LENGTH + this.hashMsg.getLengthInBytes();;
        return length;
    }

    @Override
    protected void validateMessage() {}

}
