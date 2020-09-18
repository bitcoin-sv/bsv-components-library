package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.base.TxOutputBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class TxOutputSerializer implements BitcoinSerializer<TxOutput> {
    private static TxOutputSerializer instance;

    private TxOutputSerializer() {}

    public static TxOutputSerializer getInstance() {
        if (instance == null) {
            synchronized (TxOutputSerializer.class) {
                instance = new TxOutputSerializer();
            }
        }
        return instance;
    }
    @Override
    public TxOutput deserialize(ByteArrayReader byteReader) {
        Coin value = Coin.valueOf(byteReader.readInt64LE());

        // The script length is stored oin the byte array, following the same rules as in the TxOPutputMsg.
        int scriptLen = (int) BitcoinSerializerUtils.deserializeVarInt(byteReader);
        byte[] pk_script = byteReader.read(scriptLen);

        TxOutput result = TxOutputBean.builder().value(value).scriptBytes(pk_script).build();
        return result;
    }
    @Override
    public void serialize(TxOutput object, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(object.getValue().getValue());
        byte[] script = (object.getScriptBytes() != null)? object.getScriptBytes() : new byte[]{};
        BitcoinSerializerUtils.serializeVarInt(script.length, byteWriter);
        byteWriter.write(script);
    }
}
