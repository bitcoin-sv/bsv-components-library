package com.nchain.jcl.net.protocol.messages.common;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import lombok.EqualsAndHashCode;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Bitcoin Message is a Message sent over the wire in the Blockchain Network with a
 * specific structure: a HeaderMsg and a Body. Both Header and Body extend the Message class themselves, but
 * the Header has an specific structure as defined in the P2P.
 *
 * This class is just a Wrapper, storing a reference to the Header and the Body.
 *
 * This class is immutable nd safe for multithreading
 */
@EqualsAndHashCode
public class BitcoinMsg<M extends Message> {
    private HeaderMsg header;
    private M body;

    /**
     * Constructor. Use this constructor when you have the Header and Body already.
     *
     * When you are creating the bitcoin Message from scratch, the best way is to use the BitcoinMsgBuilder, since
     * that class takes care of making the calculations that some fields of the header need.
     * But when you de-serialize the Message, you only need to read the data and popuate the header and body with it,
     * so no calculations are needed. In that case this constructor is more direct.
     */
    public BitcoinMsg(HeaderMsg header, M body) {
        this.header = header;
        this.body = body;
    }

    // getter
    public HeaderMsg getHeader() {
        return header;
    }

    // getter
    public M getBody() {
        return body;
    }

    // Convenience method. Indicates is the message represents the Commands provided
    public boolean is(String command) { return header.getCommand().equalsIgnoreCase(command);}

    /** Returns the Sice in Bytes of the Serialized version of this Message */
    public long getLengthInbytes() {
        return header.getLengthInBytes() + body.getLengthInBytes();
    }

    @Override
    public String toString() {
        return "HEAD: " + header + "\n" + "BODY: " + body;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BitcoinMsg)) return false;
        BitcoinMsg other = (BitcoinMsg) obj;
        return Objects.equal(this.header, other.header) && Objects.equal(this.body, other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.header, this.body);
    }
}
