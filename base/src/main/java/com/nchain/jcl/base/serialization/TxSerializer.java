package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.base.TxBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class TxSerializer implements BitcoinSerializer<Tx> {

    private static TxSerializer instance;

    private TxSerializer() {}

    public static TxSerializer getInstance() {
        if (instance == null) {
            synchronized (TxSerializer.class) {
                instance = new TxSerializer();
            }
        }
        return instance;
    }
    @Override
    public Tx deserialize(ByteArrayReader byteReader) {
        long version = byteReader.readUint32();
        int txInCountValue = (int) BitcoinSerializerUtils.deserializeVarInt(byteReader);
        List<TxInput> inputs = new ArrayList<>();
        for(int i =0 ; i< txInCountValue; i++) {
            inputs.add(TxInputSerializer.getInstance().deserialize(byteReader));
        }
        int txOutCountValue = (int) BitcoinSerializerUtils.deserializeVarInt(byteReader);
        List<TxOutput> outputs = new ArrayList<>();
        for(int i =0 ; i< txOutCountValue; i++) {
            outputs.add(TxOutputSerializer.getInstance().deserialize(byteReader));
        }
        long locktime = byteReader.readUint32();
        Tx result = TxBean.builder()
                .inputs(inputs)
                .outputs(outputs)
                .version(version)
                .lockTime(locktime)
                .build();
        return result;
    }

    @Override
    public void serialize(Tx object, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(object.getVersion());
        BitcoinSerializerUtils.serializeVarInt(object.getInputs().size(), byteWriter);
        for (TxInput input : object.getInputs()) TxInputSerializer.getInstance().serialize(input, byteWriter);
        BitcoinSerializerUtils.serializeVarInt(object.getOutputs().size(), byteWriter);
        for (TxOutput output : object.getOutputs()) TxOutputSerializer.getInstance().serialize(output, byteWriter);
        byteWriter.writeUint32LE(object.getLockTime());
    }


}
