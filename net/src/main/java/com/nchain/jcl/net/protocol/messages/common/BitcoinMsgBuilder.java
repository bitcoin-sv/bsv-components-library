package com.nchain.jcl.net.protocol.messages.common;

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.messages.HeaderMsg;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
 *
 * A builder to create instances of {@link BitcoinMsg}
 *
 * This Builder is the best option
 *
 */
public class BitcoinMsgBuilder<M extends Message> {

    private M bodyMsg;
    private ProtocolBasicConfig config;

    /**
     * Constructor.
     * It allows to build instances of {@link BitcoinMsg}.
     * The Header is automatically created by this builder, but the Body must be created a separate builder
     * received as a parameter.
     *
     * @param config            P2P Configuration
     * @param bodyBuilder       Builder to create instances of the Message used a Body of the Message
     */
    public BitcoinMsgBuilder(ProtocolBasicConfig config, MessageBuilder<M> bodyBuilder) {
        this(config, bodyBuilder.build());
    }

    /**
     * Constructor.
     * It allows to build instances of {@link BitcoinMsg}.
     * The Header is automatically created by this builder, and the Body is provided.
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

        // We build the header (the header must be built after the body, since some of its
        // fields depend on the body content:
        HeaderMsg header = HeaderMsg.builder()
                .command(bodyMsg.getMessageType())
                .length((int) bodyMsg.getLengthInBytes())
                .magic(config.getMagicPackage())
                .build();
        // NOTE: The CHECKSUM field is NOT Calculated here, only when we SERIALIZE the Message just before sending it...
        BitcoinMsg<M> result = new BitcoinMsg<>(header, bodyMsg);
        return result;
    }
}
