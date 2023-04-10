package io.bitcoinsv.bsvcl.net.protocol.messages;


import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BodyMessage;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayBuffer;

import java.io.Serializable;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A ByteStreamMsg containing bytes in a raw format. This can be used for direct streaming to and from the peer when we don't really care too much about the contents
 * of the message.
 *
 */
public final class ByteStreamMsg extends BodyMessage implements Serializable {

    public static final String MESSAGE_TYPE = "bytestream";
    public static ByteArrayBuffer content;

    public ByteStreamMsg(ByteArrayBuffer content) {
        super(0);
       this.content = content;
    }

    public ByteArrayBuffer getContent() {
        return content;
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }

    @Override
    protected long calculateLength() {
        return content.size();
    }

    @Override
    protected void validateMessage() {}

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))           { return false; }
        if (obj.getClass() != getClass()) { return false; }
        return true;
    }

    @Override
    public ByteStreamPartMsgBuilder toBuilder() {
        return new ByteStreamPartMsgBuilder()
                    .content(this.content);
    }

    /**
     * Builder
     */
    public static class ByteStreamPartMsgBuilder extends BodyMessageBuilder {
        private ByteArrayBuffer content;

        public ByteStreamPartMsgBuilder() {}

        public ByteStreamPartMsgBuilder content(ByteArrayBuffer content) {
            this.content = content;
            return this;
        }

        public ByteStreamMsg build() {
            return new ByteStreamMsg(content);
        }
    }
}
