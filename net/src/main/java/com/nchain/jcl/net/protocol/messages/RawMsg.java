package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Raw MEsage is a message which is NOT Deserialized/Serialized, that means that its content is the same as the
 * raw content sent to the wire, which is a byte Array.
 *
 * further extensions of this class might add custom fields.
 *
 */
public abstract class RawMsg extends Message {

    protected byte[] content;

    public RawMsg(byte[] content) {
        this.content = content;
        init();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    public byte[] getContent() {
        return this.content;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        RawMsg other = (RawMsg) obj;
        return Objects.equal(this.content, other.content);
    }

}
