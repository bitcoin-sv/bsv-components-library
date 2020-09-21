package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.bean.base.TxOutPointBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class TxOutPointSerializer implements BitcoinSerializer<TxOutPoint> {

    private static TxOutPointSerializer instance;

    private TxOutPointSerializer() {}

    public static TxOutPointSerializer getInstance() {
        if (instance == null) {
            synchronized (TxOutPointSerializer.class) {
                instance = new TxOutPointSerializer();
            }
        }
        return instance;
    }
    @Override
    public TxOutPoint deserialize(ByteArrayReader byteReader) {
        long currentReaderPosition = byteReader.getBytesReadCount();

        Sha256Wrapper hash = BitcoinSerializerUtils.deserializeHash(byteReader);
        long index = byteReader.readUint32();

        long finalReaderPosition = byteReader.getBytesReadCount();
        long objectSize = finalReaderPosition - currentReaderPosition;

        TxOutPoint result = TxOutPointBean.builder().sizeInBytes(objectSize).hash(hash).index(index).build();
        return result;
    }

    @Override
    public void serialize(TxOutPoint object, ByteArrayWriter byteWriter) {
        BitcoinSerializerUtils.serializeHash(object.getHash(), byteWriter);
        byteWriter.writeUint32LE(object.getIndex());
    }
}
