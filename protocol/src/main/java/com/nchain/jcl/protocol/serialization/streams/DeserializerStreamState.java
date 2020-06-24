package com.nchain.jcl.protocol.serialization.streams;

import com.nchain.jcl.protocol.messages.HeaderMsg;
import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.messages.common.Message;
import com.nchain.jcl.tools.streams.StreamState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-16 13:48
 *
 * This class stores the current State of a Deserializer Stream.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
public class DeserializerStreamState extends StreamState {

    /**
     * State of the Deserialization process. The bytes form a Message might not come at the same time, so every time
     * we receive new bytes, we need to keep track of the state we are and what to expect next.
     */
    enum ProcessingBytesState {
        SEEKING_HEAD,               // we are downloading bytes to get a Message Header
        SEEIKING_BODY,              // we are downloading bytes to the a Message Body (payload)
        DESERIALIZING_BODY,         // we are deserializing the Body in Real-Time
        IGNORING_BODY,              // we are ignoring the body
        CORRUPTED;                  // the data received do not make any sense. The Stream will be closed soon

        boolean isCorrupted() { return this == CORRUPTED;}  // convenience
    }

    /**
     * By Default, the Deserialization process runs in the same Thread as all the other Serialization Streams
     * connected to other Peers. So if we are connected to 30 Peers, all of them are running in the same Thread.
     * This is called the SHARED_THREAD (default).
     *
     * The Header of the Message is only deserialized once we have received all its bytes. But for the Body message,
     * The behaviour depends on the Message Size:
     *
     *  - If the Message is "small", then we WAIT until we receive all the bytes from the Body, and then we deserialize.
     *  - If the Message is "Big", we cannot afford to wait for all the bytes, since we might run out of memory. So in
     *    this case, we deserialize the Message in "real-time", meaning we process the Bytes as soon as they come,
     *    without waiting to get the whole Body first. But this "real-time" processing implies launching a new Dedicated
     *    Thread to do this "real-time" processing.
     *    When a Dedicated thread is launched, it will take care of processing all the incoming Bytes, until no more
     *    bytes come. During that time, the "normal/Shared" Thread will only feed the buffer with the new bytes
     *    coming, but it will not process them (since that work is already being carried out by the DEDICATED Thread)
     *
     *    This class is IMMUTABLE and SAFE for MULTI-THREAD
     */
    enum ThreadState {
        // There is only 1 thread running (the Shared Thread). This Thread takes care of everythign: receiving the new
        // incoming Bytes, and processing them:
        SHARED_THREAD,
        // There are 2 Threads running: The SHARED Thread will only take care of Receiving the new Bytes, The
        // DEDICATED Thread will process them.
        SHARED_AND_DEDICATED_THREAD;

        boolean dedicatedThreadRunning()    { return this == SHARED_AND_DEDICATED_THREAD;}
    }

    @Builder.Default
    private ProcessingBytesState processState = ProcessingBytesState.SEEKING_HEAD;
    @Builder.Default
    private ThreadState treadState = ThreadState.SHARED_THREAD;

    // Last Deserialized Messages:
    private HeaderMsg       currentHeaderMsg;
    private Message         currentBodyMsg;
    private BitcoinMsg<?>   currentBitcoinMsg;

    // This variable indicates whether there are still Bytes in the Buffer, and those bytes can be processed Now.
    // sometimes we DO have bytes remaining in the buffer, but we can't process just yet because we are waiting for
    // more to come (like when we deserialize a Header or a small Message). If this variable is TRUE, it means that
    // those bytes can be processed, so no need to wait more...
    @Builder.Default
    private boolean workToDoInBuffer = true;

    // If we are in IGNORING_BODY mode this stores the number of bytes still pending to ignore from the Buffer
    @Builder.Default
    private long reminingBytestoIgnore = 0;
}
