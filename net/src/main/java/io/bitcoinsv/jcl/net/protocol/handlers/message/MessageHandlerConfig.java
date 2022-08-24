package io.bitcoinsv.jcl.net.protocol.handlers.message;



import io.bitcoinsv.jcl.net.protocol.config.ProtocolBasicConfig;


import io.bitcoinsv.jcl.net.protocol.events.data.RawTxMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.events.data.TxMsgReceivedEvent;
import io.bitcoinsv.jcl.net.protocol.handlers.message.streams.deserializer.DeserializerConfig;
import io.bitcoinsv.jcl.net.protocol.messages.ByteStreamMsg;
import io.bitcoinsv.jcl.tools.handlers.HandlerConfig;

import java.util.HashMap;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It soes the configuration variables needed by the Message Handler.
 * This handler is the handler responsable for the message Serialization/Deserialization
 */
public final class MessageHandlerConfig extends HandlerConfig {

    private ProtocolBasicConfig basicConfig = ProtocolBasicConfig.builder().build(); // default

    /** If set, this object will be invoked BEFORE the DESERIALIZATION takes place */
    private final MessagePreSerializer preSerializer;

    /** Deserializer Cache Config: */
    private DeserializerConfig deserializerConfig = DeserializerConfig.builder().build();

    /** If TRUE, then the TXs are read from the wire in raw format, without Deserialization */
    private boolean rawTxsEnabled = false;

    /**
     * A Map containing Batch Message Configurations. If for example we want to Deserialize the "RawTxMsg" messages
     * in batches, then an entry with "RawTxMsg.class" as a Key should be included here.
     */
    private HashMap<Class, MessageBatchConfig> msgBatchConfigs = new HashMap<>();


    /**
     * If TRUe, then the CHECKSuM of all the INCOMING messages is calculated and checked against the "checksum" field
     * in them, i order to verity they are correct.
     *
     * NOTE: If this is TRUE, then the  "calculateChecksum" FLAG in the "DeserializerConfig" within this class must be
     * also set to TRUE.
     *
     * NOTE: This only accepts checksum for Incoming Messages. For OUTCOMING Messages, checksum is ALWAYS generated.
     */
    private boolean verifyChecksum = true; // default


    /**
     * A Testing Property.
     * If True, then ALL Peers are allowed by default to send BIG Messages to us
     * IT MUST BE DISABLED IN A PRODUCTION ENV
     */
    private boolean allowBigMsgFromAllPeers = false;

    MessageHandlerConfig(ProtocolBasicConfig basicConfig,
                         MessagePreSerializer preSerializer,
                         DeserializerConfig deserializerConfig,
                         boolean rawTxsEnabled,
                         HashMap<Class, MessageBatchConfig> msgBatchConfigs,
                         boolean verifyChecksum,
                         boolean allowBigMsgFromAllPeers
    ) {
        if (basicConfig != null)
            this.basicConfig = basicConfig;
        this.preSerializer = preSerializer;
        if (deserializerConfig != null) {
            this.deserializerConfig = deserializerConfig;
        }

        this.rawTxsEnabled = rawTxsEnabled;
        this.msgBatchConfigs = msgBatchConfigs;
        this.verifyChecksum = verifyChecksum;
        this.allowBigMsgFromAllPeers = allowBigMsgFromAllPeers;
    }

    public ProtocolBasicConfig getBasicConfig()                     { return this.basicConfig; }
    public MessagePreSerializer getPreSerializer()                  { return this.preSerializer; }
    public DeserializerConfig getDeserializerConfig()               { return this.deserializerConfig; }
    public boolean isRawTxsEnabled()                                { return this.rawTxsEnabled; }
    public HashMap<Class, MessageBatchConfig> getMsgBatchConfigs()  { return this.msgBatchConfigs;}
    public boolean isVerifyChecksum()                               { return this.verifyChecksum;}
    public boolean isAllowBigMsgFromAllPeers()                      { return this.allowBigMsgFromAllPeers;}

    @Override
    public String toString() {
        return "MessageHandlerConfig(basicConfig=" + this.getBasicConfig()
                + ", preSerializer=" + this.getPreSerializer() + ", deserializerConfig="
                + this.getDeserializerConfig()
                + ", msgBatchConfigs=" + msgBatchConfigs
                + ", verifyChecksum=" + this.verifyChecksum
                + ", allowBigMsgFromAllPeers=" + this.allowBigMsgFromAllPeers + ")";
    }

    public MessageHandlerConfigBuilder toBuilder() {
        return new MessageHandlerConfigBuilder().
                basicConfig(this.basicConfig)
                .preSerializer(this.preSerializer)
                .deserializerConfig(this.deserializerConfig)
                .rawTxsEnabled(this.rawTxsEnabled)
                .msgBatchConfigs(this.msgBatchConfigs)
                .verifyChecksum(this.verifyChecksum)
                .allowBigMsgFromAllPeers(this.allowBigMsgFromAllPeers);
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
        private HashMap<Class, MessageBatchConfig> msgBatchConfigs = new HashMap<>();
        private boolean verifyChecksum = true; // default
        private boolean allowBigMsgFromAllPeers = false;

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

        public MessageHandlerConfig.MessageHandlerConfigBuilder msgBatchConfigs(HashMap<Class, MessageBatchConfig> msgBatchConfigs) {
            this.msgBatchConfigs = msgBatchConfigs;
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder addMsgBatchConfig(Class msgType, MessageBatchConfig msgBatchConfig) {
            this.msgBatchConfigs.put(msgType, msgBatchConfig);
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder setTxsBatchConfig(MessageBatchConfig batchConfig) {
            this.msgBatchConfigs.put(TxMsgReceivedEvent.class, batchConfig);
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder setRawTxsBatchConfig(MessageBatchConfig batchConfig) {
            this.msgBatchConfigs.put(RawTxMsgReceivedEvent.class, batchConfig);
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder setRawBytesBatchConfig(MessageBatchConfig batchConfig) {
            this.msgBatchConfigs.put(ByteStreamMsg.class, batchConfig);
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder verifyChecksum(boolean verifyChecksum) {
            this.verifyChecksum = verifyChecksum;
            return this;
        }

        public MessageHandlerConfig.MessageHandlerConfigBuilder allowBigMsgFromAllPeers(boolean allowBigMsgFromAllPeers) {
            this.allowBigMsgFromAllPeers = allowBigMsgFromAllPeers;
            return this;
        }

        public MessageHandlerConfig build() {
            return new MessageHandlerConfig(basicConfig, preSerializer, deserializerConfig, rawTxsEnabled, msgBatchConfigs, verifyChecksum, allowBigMsgFromAllPeers);
        }
    }
}
