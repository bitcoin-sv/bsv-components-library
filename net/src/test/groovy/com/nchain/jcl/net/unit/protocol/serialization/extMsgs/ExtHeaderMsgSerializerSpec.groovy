package com.nchain.jcl.net.unit.protocol.serialization.extMsgs

import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.ProtocolVersion
import com.nchain.jcl.net.protocol.messages.HeaderMsg
import com.nchain.jcl.net.protocol.serialization.HeaderMsgSerializer
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import io.bitcoinj.params.MainNetParams
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
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(MainNetParams.get());
            ProtocolBasicConfig protocolBasicConfig = protocolConfig.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.SUPPORT_EXT_MSGS.getVersion())
                .build();

            // We create a Dummy Header
            HeaderMsg headerMsg = new HeaderMsg.HeaderMsgBuilder()
                .magic(1)
                .command(HeaderMsg.EXT_COMMAND)
                .length(10)
                .checksum(10)
                .extCommand(HeaderMsg.MESSAGE_TYPE)
                .extLength(5_000_000_000) // 5GB
                .build()
        when:
            // We Serialize it and deserialize it:
            SerializerContext serializerContext = SerializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()
            ByteArrayWriter writer = new ByteArrayWriter()
            HeaderMsgSerializer.getInstance().serialize(serializerContext, headerMsg, writer)
            byte[] headerBytes = writer.reader().getFullContentAndClose()

            // Now we deserialize it:
            DeserializerContext deserializerContext = DeserializerContext.builder()
                .protocolBasicConfig(protocolBasicConfig)
                .build()
            ByteArrayReader reader = new ByteArrayReader(headerBytes)
            HeaderMsg headerMsgDes = HeaderMsgSerializer.getInstance().deserialize(deserializerContext, reader)
        then:
            // And we compare both: The original Header and the one deserialized should be equal
            headerMsg.equals(headerMsgDes)
    }

}
