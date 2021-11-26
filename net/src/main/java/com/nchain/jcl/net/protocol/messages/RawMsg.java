package com.nchain.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.BodyMessage;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Raw Mesage is a message which is NOT Deserialized/Serialized, either completely or partially.
 * Messagfes extending this class can use it to store the whole content in raw format. But sometimes, its useful to
 * deserialize some information, so it can be parsed, in that case, an extending class can add its own fields, and
 * decide if its gooing ot use the "content" field here to store the WHOLE message, or only those parts that are
 * not deserialized.
 *
 */
public abstract class RawMsg extends BodyMessage implements Serializable {

    protected byte[] content;

    public RawMsg(byte[] content, byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.content = content;
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
