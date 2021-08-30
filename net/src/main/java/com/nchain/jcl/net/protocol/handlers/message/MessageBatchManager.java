package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.net.network.events.P2PEvent;
import com.nchain.jcl.net.protocol.events.data.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 23/08/2021
 *
 * A class to keep track of the state of a Batch of Messages, either to be broadcast to the network or to be broadcast
 * to the EventBus and rest of JCL components.
 *
 * If a MessageBatch config has been specified for any MsgType, then an instance of this class will be crated and it
 * will keep track of the events stored in it, so we can control when we can broadcast the whole Batch.
 */

class MessageBatchManager<E extends MsgReceivedEvent> {

    // A Static list of Supplies that create instances of XXXXBatchMsgReceivedEvent. For each different type of Msg that
    // we want to Batch, a different type of BatchMegReceivedEvent must be created, so it can be later published to the
    // EventBus

    private static Map<Class<? extends MsgReceivedEvent>, Function<List<? extends MsgReceivedEvent>, MsgReceivedBatchEvent>> BATCH_SUPPLIERS = Map.ofEntries(
            Map.entry(TxMsgReceivedEvent.class, events -> new TxsBatchMsgReceivedEvent((List<TxMsgReceivedEvent>) events)),
            Map.entry(RawTxMsgReceivedEvent.class, events -> new RawTxsBatchMsgReceivedEvent((List<RawTxMsgReceivedEvent>) events))
    );

    private Class<E> msgClass;
    private MessageBatchConfig config;
    private List<E> events = new ArrayList<>();
    private int currentEventsSize = 0;
    private Instant timestamp = Instant.now();

    /** Constructor. It assigns the MessagesBatch config */
    public MessageBatchManager(Class<E> msgClass, MessageBatchConfig config) {
        this.msgClass = msgClass;
        this.config = config;
    }

    /** It indicates if a Batch can be extracted, according to the Configuration */
    public synchronized boolean isBatchReadyToBroadcast() {
        if (events.isEmpty()) return false;

        if (config.getMaxMsgsInBatch() != null && events.size() >= config.getMaxMsgsInBatch()) {
            return true;
        } else if (config.getMaxBatchSizeInbytes() != null && currentEventsSize >= config.getMaxBatchSizeInbytes()) {
            return true;
        } else if (config.getMaxIntervalBetweenBatches() != null && Duration.between(timestamp, Instant.now()).compareTo(config.getMaxIntervalBetweenBatches()) > 0) {
            return true;
        } else {
            return false;
        }
    }

    // getter
    protected List<E> getEvents() { return this.events;}
    // getter
    protected Instant getTimestamp() { return this.timestamp;}

    /** Adds a new Event */
    public synchronized void addEvent(E event) {
        this.events.add(event);
        this.currentEventsSize += event.getBtcMsg().getLengthInbytes();
    }

    /**
     * It adds a new Event tot he Batch, and if the Batch is fullfilled according to the Configuration, it will return
     * the Btch and reset it so it can be feed with new messages after this call.
     */
    public synchronized Optional<MsgReceivedBatchEvent<E>> addEventAndExtractBatch(E event) {
        this.addEvent(event);
        if (isBatchReadyToBroadcast()) return extractBatchAndReset();
        else return Optional.empty();
    }

    /** Extract a Batch and clears up the internal, making it ready for accepting new Events */
    public synchronized Optional<MsgReceivedBatchEvent<E>> extractBatchAndReset() {
        if (this.events.isEmpty()) return Optional.empty();
        MsgReceivedBatchEvent result = BATCH_SUPPLIERS.get(msgClass).apply(this.events);
        this.events = new ArrayList<>();
        this.timestamp = Instant.now();
        this.currentEventsSize = 0;
        return Optional.of(result);
    }
}
