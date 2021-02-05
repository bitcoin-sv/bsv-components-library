package com.nchain.jcl.net.protocol.messages;



import com.nchain.jcl.net.protocol.messages.common.MessageBuilder;
import io.bitcoinj.core.Sha256Hash;
import lombok.EqualsAndHashCode;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A builder for instances of {@link RejectMsg}
 */
@EqualsAndHashCode
public class RejectMsgBuilder extends MessageBuilder<RejectMsg> {

    // This value os the Minimum Length of this message. But it might be larger depending on
    // the dynamic fields (VARStrMSg, etc):
    private static int FIXED_CCODE_LENGTH = 1;

    private VarStrMsg message;
    private RejectMsg.RejectCode ccode;
    private VarStrMsg reason;
    private byte[] data;
    private Sha256Hash dataHash;

    /** Constructor */
    public RejectMsgBuilder() {}

    /** Sets the "Message" field  */
    public RejectMsgBuilder setMessage(String message) {
        VarStrMsg newMessage = VarStrMsg.builder().str(message).build();
        return setMessage(newMessage);
    }

    /** Sets the "Message" field */
    public RejectMsgBuilder setMessage(VarStrMsg message) {
        this.message = message;
        return this;

    }

    /** It sets the Reason Code for this Message */
    public RejectMsgBuilder setCcode(RejectMsg.RejectCode ccode) {
        this.ccode = ccode;
        return this;
    }

    /** It sets the Reason. */
    public RejectMsgBuilder setReason(String reason) {
        VarStrMsg newReason = VarStrMsg.builder().str(reason).build();
        return setReason(newReason);
    }

    /** It sets the Reason. */
    public RejectMsgBuilder setReason(VarStrMsg reason) {
        this.reason = reason;
        return this;
    }

    /** It assigns the "data" field as the HASH of a TX or Block */
    public RejectMsgBuilder setData(Sha256Hash data) {
        this.data = null;
        this.dataHash = data;
        return this;
    }

    /** It assigns the "data" field as a general byte array (generic scenario) */
    public RejectMsgBuilder setData(byte[] data) {
        this.data = data;
        this.dataHash = null;
        return this;
    }

    @Override
    public RejectMsg build() {
              return new RejectMsg(message, ccode, reason, dataHash, data);
    }

}
