package io.bitcoinsv.jcl.net.protocol.messages.common;


import com.google.common.base.Objects;
import io.bitcoinsv.bitcoinjsv.core.Utils;

import java.util.Arrays;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A BodyMessage is a Mesage that represents the PAYLOAD/BODY of a Message. So the whole payload can be stored in
 * this object. We can then combine a "HeaderMsg" + "BodyMsg" to build a "BitcoinMsg".
 *
 * Most of the times, a BodyMsg class will be composed of other inner Message classes that together make up the
 * whole payload. The BodyMessage also provides some functionalities that are specific of a BODY, and are NOT present
 * in individual or smaller message classes:
 * - checksum:    We store an extra field wth the checksum calculated out of the whole Body (using double Sha256)
 * - extraBytes: This is just fail-safe measure: If this implementation is not upt-to-date with other nodes
 *               implementation, we might receive Messages which BODY has MORE Bytes than we need to deserialze,
 *               like bytes added after a protocol upgrade, an upgrade that we have NOT developed yet. In that case,
 *               we store those extraBytes in this field, so at least the buffer is "clean" after deserializing the
 *               Body so we can read more incoming messages from it.
 */
public abstract class BodyMessage extends Message {

    // Bytes received from the network but NOT used during the Deserialization. They usually come at the end
    // of the message, if any. We store them here in case we need to Serialize the message later on and keeping
    // the original structure.
    protected byte[] extraBytes = Utils.EMPTY_BYTE_ARRAY;

    // checksum calculated out of the Message bytes. Its NOT part of the physical message on the wire and it
    // might be or not populated based on configuration (checksum verification can be disabled)
    protected long checksum;

    /** Constructor */
    public BodyMessage(byte[] extraBytes, long checksum) {
        this.extraBytes = extraBytes;
        this.checksum = checksum;
    }

    /** Constructor */
    public BodyMessage(long checksum) {
        this(Utils.EMPTY_BYTE_ARRAY, checksum);
    }

    @Override
    public void init() {
        validateMessage();
        this.lengthInBytes = calculateLength() + extraBytes.length;
    }
    // getters:
    public byte[] getExtraBytes()   { return this.extraBytes;}
    public long getChecksum()       { return this.checksum;}

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        BodyMessage other = (BodyMessage) obj;
        return Arrays.equals(this.extraBytes, other.extraBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.extraBytes);
    }

    /** Returns the builder if we want to modify the content (it will crate a new instance) */
    abstract public BodyMessageBuilder toBuilder();

    /**
     * Abstract Builder that can be extended by sub-classes
     */
    public static abstract class BodyMessageBuilder {
        protected byte[] extraBytes = Utils.EMPTY_BYTE_ARRAY;
        protected long checksum;

        public BodyMessageBuilder() {}

        public BodyMessageBuilder(byte[] extraBytes, long checksum) {
            this.checksum = checksum;
            this.extraBytes = extraBytes;
        }

        public BodyMessageBuilder checksum(long checksum) {
            this.checksum = checksum;
            return this;
        }

        public BodyMessageBuilder extraBytes(byte[] extraBytes) {
            this.extraBytes = extraBytes;
            return this;
        }

        public abstract Message build();
    }
}
