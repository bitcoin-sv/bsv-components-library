package io.bitcoinsv.bsvcl.tools.bigObjects.receivers.events;


import io.bitcoinsv.bsvcl.tools.events.Event;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An event triggered by a "BigObject Receiver" when a CHUNK of Items of that Object has been received.
 *
 * @param <I> Type of each Item received
 */
public class BigObjectItemsReceivedEvent<I> extends Event {
    private String objectId;
    private List<I> items;
    private String source;

    public BigObjectItemsReceivedEvent(String objectId, List<I> items, String source) {
        this.objectId = objectId;
        this.items = items;
        this.source = source;
    }

    public String getObjectId() { return this.objectId;}
    public List<I> getItems()   { return this.items;}
    public String getSource()   { return this.source;}

}