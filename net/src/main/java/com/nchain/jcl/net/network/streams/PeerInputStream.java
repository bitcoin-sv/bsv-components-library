package com.nchain.jcl.net.network.streams;

import com.nchain.jcl.base.tools.streams.InputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A PeerInputStream is like a regular InputStream, but adding more information about the
 * Peer this Stream is connected to, and it's State (those methods are defined in the
 * "PeerStreamInfo" interface).
 */
public interface PeerInputStream<T> extends InputStream<T>, PeerStreamInfo { }
