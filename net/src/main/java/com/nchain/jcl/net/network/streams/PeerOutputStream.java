package com.nchain.jcl.net.network.streams;

import com.nchain.jcl.base.tools.streams.OutputStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 16:28
 *
 * A PeerOutputStream is like a regular OutputStream, but adding more information about the
 * Peer this Stream is connected to, and it's State (those methods are defined in the
 * "PeerStreamInfo" interface).
 */
public interface PeerOutputStream<T> extends OutputStream<T>, PeerStreamInfo {}
