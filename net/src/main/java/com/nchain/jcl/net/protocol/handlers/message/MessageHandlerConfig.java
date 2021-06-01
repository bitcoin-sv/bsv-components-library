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

    private ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder().build(); // default

    /** If set, this object will be invoked BEFORE the DESERIALIZATION takes place */
    private final MessagePreSerializer preSerializer;

    /** Deserializer Cache Config: if TRue, run-time statistics about the Cache are generated (only for testing)*/
    private DeserializerConfig deserializerConfig = DeserializerConfig.builder().build();

    /** If TRUE, then the TXs are read from the wire in raw format, without Deserialization */
    private boolean rawTxsEnabled = false;

    /**
     * Maximun number of Connections to other Peers that can use a dedicated thread to manage its connections.
     * By default, all the connections to remote peers are managed by a single Thread, that's why JCL can connect to
     * so many peers in parallel. but sometimes its worth it to manage an individual connection with a dedicated Thread
     * , for example when there is a big Message coming from that connection.
     */
    private int maxNumberDedicatedConnections = 10;

    MessageHandlerConfig(ProtocolBasicConfig basicConfig,
                         MessagePreSerializer preSerializer,
                         DeserializerConfig deserializerConfig,
                         boolean rawTxsEnabled,
                         int maxNumberDedicatedConnections
    ) {
        if (basicConfig != null)
            this.basicConfig = basicConfig;
        this.preSerializer = preSerializer;
        if (deserializerConfig != null)
            this.deserializerConfig = deserializerConfig;
        this.rawTxsEnabled = rawTxsEnabled;
        this.maxNumberDedicatedConnections = maxNumberDedicatedConnections;
    }

    public ProtocolBasicConfig getBasicConfig()         { return this.basicConfig; }
    public MessagePreSerializer getPreSerializer()      { return this.preSerializer; }
    public DeserializerConfig getDeserializerConfig()   { return this.deserializerConfig; }
    public boolean isRawTxsEnabled()                    { return this.rawTxsEnabled; }
    public int getMaxNumberDedicatedConnections()       { return this.maxNumberDedicatedConnections;}

    @Override
    public String toString() {
        return "MessageHandlerConfig(basicConfig=" + this.getBasicConfig() + ", preSerializer=" + this.getPreSerializer() + ", deserializerConfig=" + this.getDeserializerConfig() + ", maxNumberDedicatedConnections=" + maxNumberDedicatedConnections + ")";
    }

    public MessageHandlerConfigBuilder toBuilder() {
        return new MessageHandlerConfigBuilder().
                basicConfig(this.basicConfig)
                .preSerializer(this.preSerializer)
                .deserializerConfig(this.deserializerConfig)
                .rawTxsEnabled(rawTxsEnabled)
                .maxNumberDedicatedConnections(this.maxNumberDedicatedConnections);
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
        private boolean rawTxsEnabled = false;
        private int maxNumberDedicatedConnections = 10;

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

        public MessageHandlerConfig.MessageHandlerConfigBuilder rawTxsEnabled(boolean rawTxsEnabled) {
            this.rawTxsEnabled = rawTxsEnabled;
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder maxNumberDedicatedConnections(int maxNumberDedicatedConnections) {
            this.maxNumberDedicatedConnections = maxNumberDedicatedConnections;
            return this;
        }

        public MessageHandlerConfig build() {
            return new MessageHandlerConfig(basicConfig, preSerializer, deserializerConfig, rawTxsEnabled, maxNumberDedicatedConnections);
        }
    }
}
