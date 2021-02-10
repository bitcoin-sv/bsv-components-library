package com.nchain.jcl.net.protocol.handlers.message;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.net.protocol.streams.deserializer.DeserializerConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It soes the configuration variables needed by the Message Handler
 */
public final class MessageHandlerConfig extends HandlerConfig {

    private final ProtocolBasicConfig basicConfig;

    /** If set, this object will be invoked BEFORE the DESERIALIZATION takes place */
    private final MessagePreSerializer preSerializer;

    /** Deserializer Cache Config: if TRue, run-time statistics about the Cache are generated (only for testing)*/
    private DeserializerConfig deserializerConfig = DeserializerConfig.builder().build();

    MessageHandlerConfig(ProtocolBasicConfig basicConfig, MessagePreSerializer preSerializer, DeserializerConfig deserializerConfig) {
        this.basicConfig = basicConfig;
        this.preSerializer = preSerializer;
        if (deserializerConfig != null)
            this.deserializerConfig = deserializerConfig;
    }

    public ProtocolBasicConfig getBasicConfig()         { return this.basicConfig; }
    public MessagePreSerializer getPreSerializer()      { return this.preSerializer; }
    public DeserializerConfig getDeserializerConfig()   { return this.deserializerConfig; }

    @Override
    public String toString() {
        return "MessageHandlerConfig(basicConfig=" + this.getBasicConfig() + ", preSerializer=" + this.getPreSerializer() + ", deserializerConfig=" + this.getDeserializerConfig() + ")";
    }

    public MessageHandlerConfigBuilder toBuilder() {
        return new MessageHandlerConfigBuilder().basicConfig(this.basicConfig).preSerializer(this.preSerializer).deserializerConfig(this.deserializerConfig);
    }

    public static MessageHandlerConfigBuilder builder() {
        return new MessageHandlerConfigBuilder();
    }

    /**
     * Builder
     */
    public static class MessageHandlerConfigBuilder {
        private ProtocolBasicConfig basicConfig;
        private MessagePreSerializer preSerializer;
        private DeserializerConfig deserializerConfig;

        MessageHandlerConfigBuilder() { }

        public MessageHandlerConfig.MessageHandlerConfigBuilder basicConfig(ProtocolBasicConfig basicConfig) {
            this.basicConfig = basicConfig;
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder preSerializer(MessagePreSerializer preSerializer) {
            this.preSerializer = preSerializer;
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder deserializerConfig(DeserializerConfig deserializerConfig) {
            this.deserializerConfig = deserializerConfig;
            return this;
        }

        public MessageHandlerConfig build() {
            return new MessageHandlerConfig(basicConfig, preSerializer, deserializerConfig);
        }
    }
}
