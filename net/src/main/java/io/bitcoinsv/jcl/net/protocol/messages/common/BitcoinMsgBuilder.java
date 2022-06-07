package io.bitcoinsv.jcl.net.protocol.messages.common;

import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;
import io.bitcoinsv.jcl.net.protocol.config.ProtocolVersion;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A byteArray to create instances of {@link BitcoinMsg}
 *
 * This Builder is the best option
 *
 */
public class BitcoinMsgBuilder<M extends BodyMessage> {

    private M bodyMsg;
    private ProtocolBasicConfig config;
    private String msgType = null;
    private Long msgLen = null;

    /**
     * Constructor.
     * It allows to build instances of {@link BitcoinMsg}.
     * The Header is automatically created by this byteArray, and the Body is provided.
     *
     * @param config            P2P Configuration
     * @param bodyMsg           body Msg
     */
    public BitcoinMsgBuilder(ProtocolBasicConfig config, M bodyMsg) {
        this.config = config;
        this.bodyMsg = bodyMsg;
    }

    /**
     * This message may be one of many if we break down the data into a stream of messages, therefore we may want to override the message type in some instances
     * @param msgType
     * @return
     */
    public BitcoinMsgBuilder overrideHeaderMsgType(String msgType){
        this.msgType = msgType;
        return this;
    }

    /**
     * This message may be one of many if we break down the data into a stream of messages, therefore we may want to override the length in some instances
     * @param length
     * @return
     */
    public BitcoinMsgBuilder overrideHeaderMsgLength(long length) {
        this.msgLen = length;
        return this;
    }

    /**
     * Creates a new instance of a {@link BitcoinMsg}. It automatically builds the Header of the Message.
     * The construction of the Body is let to the body Builder passed in the Constructor
     */
    public BitcoinMsg<M> build() {

        // Sanity check for >4GB Messages:
        if (bodyMsg.getLengthInBytes() >= config.getThresholdSizeExtMsgs() && config.getProtocolVersion() < ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
            throw new RuntimeException("Trying to build a message bigger than 4GB with a wrong protocol Version");

        // We build the header (the header must be built after the body, since some of its
        // fields depend on the body content.
        HeaderMsg.HeaderMsgBuilder headerMsgBuilder = HeaderMsg.builder();
        headerMsgBuilder.magic(config.getMagicPackage());

        //check if the header has been overridden
        String msgType = bodyMsg.getMessageType();
        long msgLen = bodyMsg.getLengthInBytes();

        if(this.msgType != null) {
            msgType = this.msgType;
        }

        if(this.msgLen != null) {
            msgLen = this.msgLen;
        }

        // If the message is a Big one, we use extra fields (enabled after 70016)
        if (msgLen>= config.getThresholdSizeExtMsgs()) {
            headerMsgBuilder.command(HeaderMsg.EXT_COMMAND);
            headerMsgBuilder.length(HeaderMsg.EXT_LENGTH);
            headerMsgBuilder.extCommand(msgType);
            headerMsgBuilder.extLength(msgLen);
        } else {
            headerMsgBuilder.command(msgType);
            headerMsgBuilder.length((int) msgLen);
        }

        HeaderMsg header = headerMsgBuilder.build();
        // NOTE: The CHECKSUM field is NOT Calculated here, only when we SERIALIZE the Message just before sending it,
        // in the BitcoinMsgSerializerImpl...
        BitcoinMsg<M> result = new BitcoinMsg<>(header, bodyMsg);
        return result;
    }
}
