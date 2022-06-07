package io.bitcoinsv.jcl.net.protocol.messages.common;


import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class represents any piece of data that can be Serialize and sent over the wire
 * as part of a Bitcoin Message. It can be just a part of the message, or the full message.
 *
 * Every piece of information that is sent over the wire, must extend this class. If the information is complex,
 * consisting of several different fields, we can either:
 *  - Define all the fields in the same class
 *  - Or identify some reusable fields and create a separate "Message" class for them. Then we can "import" an
 *    instance of that class into the bigger one. This way, we can compose a Message out of "small" and
 *    reusable messages.
 *
 * So everything is a Message. For convenience, some specific subclases are also defined like "BodyMessage"
 * or "BitcoinMsg".
 *
 * Classes that extend this class should be immutable.
 */
public abstract class Message implements Serializable {

    // Size of the Message in Bytes. This stores the number of bytes that this Message will take once it's
    // serialized.
    protected long lengthInBytes;

    /** Constructor */
    public Message() {}

    // getter:
    public long getLengthInBytes()   { return lengthInBytes; }

    /** initialize the message length and validate its values */
    public void init() {
        validateMessage();
        this.lengthInBytes = calculateLength();
    }

    /**
     * Returns the Identifier of the Type of this message. Each subclass wil have to provide an implementation
     * returning a Value. For the "body" messages, this is the value that will be stored in the "COMMAND" field
     * in the Header, so its value must meet the specification in the Bitcoin P2P.
     * If the Message is not a "Body" message, then this value is ony informative.
     */
    abstract public String getMessageType();

    /** calculates the length of the message. To override in extending classes */
    abstract protected long calculateLength();

    /** calculates the length of the message. To override in extending classes */
    abstract protected void  validateMessage();

    @Override
    public boolean equals(Object obj) {
        if (obj == null)                  { return false; }
        if (obj == this)                  { return true; }
        if (obj.getClass() != getClass()) { return false; }

        Message other = (Message) obj;
        return Objects.equal(this.lengthInBytes, other.lengthInBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lengthInBytes);
    }

}
