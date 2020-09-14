package com.nchain.jcl.base.wiki;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.domain.api.base.TxInput;
import com.nchain.jcl.base.domain.api.base.TxOutPoint;
import com.nchain.jcl.base.domain.api.base.TxOutput;
import com.nchain.jcl.base.domain.bean.base.TxInputBean;
import com.nchain.jcl.base.serialization.BitcoinSerializerFactory;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-14
 */
public class Wiki {

    @Ignore
    public void test1() {
        TxOutput txOutput = TxOutput.builder()
                                .value(Coin.valueOf(10))
                                .scriptBytes(new byte[10])
                                .build();
    }

    @Ignore
    public void test2() {
        TxInput txInput = TxInput.builder()
                            .scriptBytes(new byte[10])
                            .sequenceNumber(1)
                            .value(Coin.valueOf(5))
                            .outpoint(TxOutPoint.builder()
                                    .hash(Sha256Wrapper.wrap(new byte[32]))
                                    .index(1)
                                    .build())
                            .build();
    }

    @Ignore
    public void test3() {
        Tx tx = Tx.builder("010000000193e3073ecc1d27f17e3d287ccefdfdba5f7d8c160242dbcd547b18baef12f9b31a")
                .build();
    }

    @Test
    public void test4() {
        Tx originalTx = Tx.builder().lockTime(2).build();
        Tx changedTx = originalTx.toBuilder()
                .version(2)
                .build();
        System.out.println("end.");
    }

    @Ignore
    public void test5() {
        TxInput input1      = TxInput.builder().build();
        TxOutput output1    = TxOutput.builder().build();
        Tx tx = Tx.builder()
                    .version(1)
                    .lockTime(100)
                    .inputs(Arrays.asList(input1))
                    .outputs(Arrays.asList(output1))
                    .build();
    }

    @Ignore
    public void test6() {
        TxInput txInput = null;
        byte[] txInputSerialed = BitcoinSerializerFactory.serialize(txInput);

        TxOutput txOutput = null;
        byte[] txOutputSerialized = BitcoinSerializerFactory.serialize(txOutput);

        Tx tx = null;
        byte[] txSerialzied = BitcoinSerializerFactory.serialize(tx);
    }

    @Ignore
    public void test7() {
        String TX_INPUT_HEX = "";
        String TX_OUTPUT_HEX = "";
        String TX_HEX = "";

        TxInput txInput = (TxInput) BitcoinSerializerFactory.deserialize(TxInput.class, TX_INPUT_HEX);
        TxOutput txOutput = (TxOutput) BitcoinSerializerFactory.deserialize(TxOutput.class, TX_INPUT_HEX);
        Tx tx = (Tx) BitcoinSerializerFactory.deserialize(Tx.class, TX_INPUT_HEX);
    }

}
