package io.bitcoinsv.bsvcl.net.unit.protocol.serialization.extMsgs


import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * Testing class for the Serialization of a HeaderMessage that suports messages bigger than 4GB, so it includes
 * the additional fields ("extCommand" and "extLength" defined in protocol 70016).
 *
 */
class ExtHeaderMsgSerializerSpec extends Specification {

    /**
     * We serialize a Header including "ext" fields and deserialize it, comparing both
     */
    def "testing Serialization & Deserialization"() {
        given:
            // We use MAINNET with 70016 Protocol Version:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(MainNetParams.get());
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig protocolBasicConfig = protocolConfig.getBasicConfig().toBuilder()
                .protocolVersion(io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion.ENABLE_EXT_MSGS.getVersion())
                .build();

            // We create a Dummy Header
            io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg headerMsg = new io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg.HeaderMsgBuilder()
                .magic(1)
                .command(io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg.EXT_COMMAND)
                .length(10)
                .checksum(10)
                .extCommand(io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg.MESSAGE_TYPE)
                .extLength(5_000_000_000) // 5GB
                .build()
        when:
            // We Serialize it and deserialize it:
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext serializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()
            ByteArrayWriter writer = new ByteArrayWriter()
            io.bitcoinsv.bsvcl.net.protocol.serialization.HeaderMsgSerializer.getInstance().serialize(serializerContext, headerMsg, writer)
            byte[] headerBytes = writer.reader().getFullContentAndClose()

            // Now we deserialize it:
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext deserializerContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()
            ByteArrayReader reader = new ByteArrayReader(headerBytes)
            io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg headerMsgDes = io.bitcoinsv.bsvcl.net.protocol.serialization.HeaderMsgSerializer.getInstance().deserialize(deserializerContext, reader)
        then:
            // And we compare both: The original Header and the one deserialized should be equal
            headerMsg.equals(headerMsgDes)
    }

}
