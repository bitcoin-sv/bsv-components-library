package com.nchain.jcl.network.streams;

import com.nchain.jcl.tools.streams.InputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:27
 *
 * A PeerInputStream is like a regular InputStream, but adding more information about the
 * Peer this Stream is connected to, and it's State (those methods are defined in the
 * "PeerStreamInfo" interface).
 */
public interface PeerInputStream<T> extends InputStream<T>, PeerStreamInfo { }
