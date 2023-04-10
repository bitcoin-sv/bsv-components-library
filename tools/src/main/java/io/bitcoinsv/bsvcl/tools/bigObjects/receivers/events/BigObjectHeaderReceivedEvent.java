package io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events;


import io.bitcoinsv.bsvcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An event triggered by a "BigObjectHeaderPlusReceiver" when the Head of the Object has been received.
 */
public class BigObjectHeaderReceivedEvent<H> extends Event {
    private String objectId;
    private H header;
    private String source;

    public BigObjectHeaderReceivedEvent(String objectId, H header, String source) {
        this.objectId = objectId;
        this.header = header;
        this.source = source;
    }

    public String getObjectId() { return this.objectId;}
    public H getHeader()        { return this.header;}
    public String getSource()   { return this.source;}

}