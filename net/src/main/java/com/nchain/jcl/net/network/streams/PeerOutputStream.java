package com.nchain.jcl.net.network.streams;


import com.nchain.jcl.tools.streams.OutputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A PeerOutputStream is like a regular OutputStream, but adding more information about the
 * Peer this Stream is connected to, and it's State (those methods are defined in the
 * "PeerStreamInfo" interface).
 */
public interface PeerOutputStream<T> extends OutputStream<T>, PeerStreamInfo {}
