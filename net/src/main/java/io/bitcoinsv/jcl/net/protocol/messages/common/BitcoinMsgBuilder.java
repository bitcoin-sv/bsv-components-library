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

        // If the message is a Big one, we use extra fields (enabled after 70016)
        if (bodyMsg.getLengthInBytes() >= config.getThresholdSizeExtMsgs()) {
            headerMsgBuilder.command(HeaderMsg.EXT_COMMAND);
            headerMsgBuilder.length(HeaderMsg.EXT_LENGTH);
            headerMsgBuilder.extCommand(bodyMsg.getMessageType());
            headerMsgBuilder.extLength(bodyMsg.getLengthInBytes());
        } else {
            headerMsgBuilder.command(bodyMsg.getMessageType());
            headerMsgBuilder.length((int) bodyMsg.getLengthInBytes());
        }

        HeaderMsg header = headerMsgBuilder.build();
        // NOTE: The CHECKSUM field is NOT Calculated here, only when we SERIALIZE the Message just before sending it,
        // in the BitcoinMsgSerializerImpl...
        BitcoinMsg<M> result = new BitcoinMsg<>(header, bodyMsg);
        return result;
    }
}