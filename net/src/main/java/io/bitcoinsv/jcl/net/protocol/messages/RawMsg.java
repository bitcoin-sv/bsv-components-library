package io.bitcoinsv.jcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.Arrays;

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
        return Objects.hashCode(super.hashCode(), Arrays.hashCode(content));
    }

    public byte[] getContent() {
        return this.content;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        RawMsg other = (RawMsg) obj;
        return Arrays.equals(this.content, other.content);
    }

}
