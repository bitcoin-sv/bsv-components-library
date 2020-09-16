package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.FullBlock;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.bean.base.FullBlockBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-16
 */
@Deprecated
public class FullBlockSerializer implements BitcoinSerializer<FullBlock> {
    private static FullBlockSerializer instance;

    private FullBlockSerializer() {}

    public static FullBlockSerializer getInstance() {
        if (instance == null) {
            synchronized (FullBlockSerializer.class) {
                instance = new FullBlockSerializer();
            }
        }
        return instance;
    }

    @Override
    public  FullBlock deserialize(ByteArrayReader byteReader) {
        // First we deserialize the Block Header
        BlockHeader header = BlockHeaderSerializer.getInstance().deserialize(byteReader);
        // The number of Txs:
        long numTxs = BitcoinSerializerUtils.deserializeVarInt(byteReader);
        // Each one of the TXs:
        List<Tx> txs = new ArrayList<>();
        for (int i = 0; i < numTxs; i++) {
            Tx tx = TxSerializer.getInstance().deserialize(byteReader);
            txs.add(tx);
        }

        // We build the Metadata:
        BlockMeta blockMeta = BlockMeta.builder().txCount(txs.size()).build();

        // We build th Block and return:
        FullBlockBean result = FullBlock.builder()
                .header(header)
                .metaData(blockMeta)
                .transactions(txs)
                .build();

        return result;
    }

    @Override
    public void serialize(FullBlock object, ByteArrayWriter byteWriter) {
        // First we serialize the block Header:
        BlockHeaderSerializer.getInstance().serialize(object.getHeader(), byteWriter);
        // The number of Txs:
        BitcoinSerializerUtils.serializeVarInt(object.getTransactions().size(), byteWriter);
        // Each one of the TXs:
        for (Tx tx: object.getTransactions())
            TxSerializer.getInstance().serialize(tx, byteWriter);
    }

}
