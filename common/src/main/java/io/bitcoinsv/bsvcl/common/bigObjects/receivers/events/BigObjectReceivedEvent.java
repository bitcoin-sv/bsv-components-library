package io.bitcoinsv.bsvcl.common.bigObjects.receivers.events;


import io.bitcoinsv.bsvcl.common.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An event triggered by a "BigObject Receiver" when The WHOLE Object has been received.
 *
 */
public class BigObjectReceivedEvent extends Event {
    private String objectId;
    private String source;

    public BigObjectReceivedEvent(String objectId, String source) {
        this.objectId = objectId;
        this.source = source;
    }

    public String getObjectId() { return this.objectId;}
    public String getSource()   { return this.source;}

}