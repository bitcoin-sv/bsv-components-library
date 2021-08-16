/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.messages.common;

import java.io.Serializable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class represents any piece of data that can be Serialize and sent over the wire
 * as part of a Bitcoin Message. It can be just a part of the message, or the full message (including Header),
 *
 * Every piece of information that is sent over the wire, must extend this class. If the information is complex,
 * consisting of several different fields, we can either:
 *  - Define all the fields in the same class
 *  - Or identify some reusable fields and create a separate "Message" class for them. Then we can "import" an
 *    instance of that class into the bigger one. This way, we can compose a Message out of "small" and
 *    reusable messages.
 *
 * So everything is a Message. Sometimes along the code, a distinction is used to differentiate them, like:
 *
 *  - Bitcoin Message: This is aFull connection-compliant message, including a Header and a Body
 *  - A Header: An special Message with a pre-defined structure that works as a Header in aBitcoin Message
 *  - A Body: A Message that goes in the "Body" part in a BitcoinMessage (like Verack, Transaction,etc)
 *
 * Classes that extend this class should be immutable.
 */
public abstract class Message implements Serializable {

    // Size of the Message in Bytes. This stores the number of bytes that this Message will take once it's
    // serialized.
    protected long lengthInBytes;

    // getter
    public long getLengthInBytes() {
        return lengthInBytes;
    }

    // updates the length
    public void updateLength(long length) {
         lengthInBytes = length;
    }

    // initialize the message length and validate its values
    public void init() {
        validateMessage();
        long length = calculateLength();
        updateLength(length);
    }

    /**
     * Returns the Identifier of the Type of this message. Each subclass wil have to provide an implementation
     * returning a Value. For the "body" messages, this is the value that will be stored in the "COMMAND" field
     * in the Header, so its value must meet the specification in the Bitcoin P2P.
     * If the Message is not a "Body" message, then this value is ony informative.
     */
    abstract public String getMessageType();

    // calculates the length of the message
    abstract protected long calculateLength();

    // calculates the length of the message
    abstract protected void  validateMessage();

}
