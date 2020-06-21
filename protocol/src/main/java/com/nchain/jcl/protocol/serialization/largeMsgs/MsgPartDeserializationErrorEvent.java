package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:14
 */
@Getter
@AllArgsConstructor
public class MsgPartDeserializationErrorEvent extends Event {
    private Exception exception;
}
