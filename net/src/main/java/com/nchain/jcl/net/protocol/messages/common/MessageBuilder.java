package com.nchain.jcl.net.protocol.messages.common;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
 *
 * A builder to create instances of {@link Message}.
 * This is the base class that must be extended. for each Message class, an specific builder must be implemented.
 */
public abstract class MessageBuilder<M extends Message> {

    /**
     * Creates a new immutable instance of a {@link Message}.
     * Each subclass must provide a valid implementation.
     */
    abstract public M build();
}
