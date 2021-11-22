package com.nchain.jcl.net.protocol.messages.common;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Messages extending this class are "artifial" messages that are NOT part of the Bitcoin P2P protocol, but
 * created for JCL. They are usualy used when the original message is too big so its been broken down into
 * these "partial" messages and broadcasts throguht JCL and its clients.
 *
 * When JCL detects a "Big" Message coming down the wire, it uses a "Large" Deserializer to deserialize it and
 * the message is broken down and broadcasts into smaller parts, each one extends "PartialMessage".
 */
public abstract class PartialMessage extends Message {}
