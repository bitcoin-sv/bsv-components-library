package io.bitcoinsv.bsvcl.net.protocol.messages.common;

import java.util.stream.Stream;

/**
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 *
 * @author m.fletcher@nchain.com
 * @date 17/05/2022
 */
public class StreamRequest {

    private final String msgType;
    private final Stream<byte[]> stream;
    private long len;

    public StreamRequest(String msgType, Stream<byte[]> stream, long len) {
        this.msgType = msgType;
        this.stream = stream;
        this.len = len;
    }

    public Stream<byte[]> getStream() {
        return stream;
    }

    public String getMsgType() {
        return msgType;
    }

    public long getLen() {
        return len;
    }

}
