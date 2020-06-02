package com.nchain.jcl.protocol.serialization.common;

import com.nchain.jcl.protocol.messages.common.Message;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-17 12:14
 *
 * Basic implementation of the "killable" feature in a Serializer. It only provides a Boolean
 * variable which values can be set to "true/needs to be killed", and which value can be inspected
 * at any time by the implementation of this intterface, and proceed accordingly in that case.
 */
public abstract class MessageSerializerKillable<M extends Message> implements MessageSerializer<M>  {
    @Getter
    private boolean hasBeenKilled = false;

    @Override
    public void kill() {
        hasBeenKilled = true;
    }

    @Override
    public boolean isKillable() { return true; }

}
