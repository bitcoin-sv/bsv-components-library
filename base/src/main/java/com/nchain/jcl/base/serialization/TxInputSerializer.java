package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.base.TxInputBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class TxInputSerializer implements BitcoinSerializer<TxInput> {
    private static TxInputSerializer instance;

    private TxInputSerializer() {}

    public static TxInputSerializer getInstance() {
        if (instance == null) {
            synchronized (TxInputSerializer.class) {
                instance = new TxInputSerializer();
            }
        }
        return instance;
    }

    @Override
    public TxInput deserialize(ByteArrayReader byteReader) {
        TxOutPoint outPoint = TxOutPointSerializer.getInstance().deserialize(byteReader);
        int scriptLen = (int) BitcoinSerializerUtils.deserializeVarInt(byteReader);
        byte[] sig_script = byteReader.read(scriptLen);
        long sequence = byteReader.readUint32();
        TxInput result = TxInputBean.builder().outpoint(outPoint).scriptBytes(sig_script).sequenceNumber(sequence).build();
        return result;
    }

    @Override
    public void serialize(TxInput object, ByteArrayWriter byteWriter) {
        TxOutPointSerializer.getInstance().serialize(object.getOutpoint(), byteWriter);
        byte[] script = (object.getScriptBytes() != null) ? object.getScriptBytes() : new byte[]{};
        BitcoinSerializerUtils.serializeVarInt(script.length, byteWriter);
        byteWriter.write(script);
        byteWriter.writeUint32LE(object.getSequenceNumber());
        // TODO: What about the VALUE??
    }
}
