package io.bitcoinsv.jcl.net.network.events;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-03-29
 *
 * Base class for any P2PRequest. distinction between P2PEvents and P2PRequests is: a P2P Event is something that
 * HAPPENED, while a P2PRequest is something you WANT to happen.
 */
public class P2PRequest extends P2PEvent {}
