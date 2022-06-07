package io.bitcoinsv.jcl.tools.common;


import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.*;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.base.*;
import io.bitcoinsv.bitcoinjsv.core.Coin;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.bitcoinjsv.core.Utils;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Utility Class with useful methods for Testing
 */
public class TestingUtils {

    /** Convenience method to generate a (Random) Hash */
    public static String buildRandomHash() {
        byte[] bytes = new byte[32];
        Random rand = new Random();
        for (int i = 0; i < 32 ; i++) bytes[i] = (byte) rand.nextInt(255);
        return Utils.HEX.encode(bytes);
    }

    /** Convenience method to generate a Dummy Block */
    public static HeaderReadOnly buildBlock(String parentHashHex) {
        Sha256Hash parentHashFinal = (parentHashHex != null) ? Sha256Hash.wrap(parentHashHex) : Sha256Hash.ZERO_HASH;
        HeaderBean result = new HeaderBean((AbstractBlock) null);
        result.setMerkleRoot(Sha256Hash.wrap(buildRandomHash()));
        result.setPrevBlockHash(parentHashFinal);
        result.setNonce(1);
        result.setDifficultyTarget(486604799);
        result.setTime(Instant.now().getEpochSecond());
        result.makeImmutable();
        return result;
    }

    /** Convenience method to build a Block with NO Parent */
    public static HeaderReadOnly buildBlock() {
        return buildBlock(null);
    }

    /** Convenience method to generate a Dummy Tx, specifying a parent Tx */
    public static Tx buildTx(String parentTxHash) {
        Random rand = new Random();
        // Dummy Tx with one input and one Output:
        // If "parentTxHash" is not null, we use it as the Hash of the TX which output we are spending
        String parentOutputHash = (parentTxHash != null) ? parentTxHash : buildRandomHash();

        TxBean result = new TxBean((AbstractBlock) null);
        result.setVersion(1);
        result.setLockTime(2);
        List<TxInput> inputs = new ArrayList<>();

        TxInput txInput1 = new TxInputBean(result);
        txInput1.setSequenceNumber(rand.nextInt(100));
        txInput1.setScriptBytes(new byte[0]);

        TxOutPoint txOutpoint1 = new TxOutPointBean(txInput1);
        txOutpoint1.setIndex(rand.nextInt(10));
        txOutpoint1.setHash(Sha256Hash.wrap(parentOutputHash));

        txInput1.setOutpoint(txOutpoint1);
        inputs.add(txInput1);
        result.setInputs(inputs);

        List<TxOutput> outputs = new ArrayList<>();

        TxOutput txOutput = new TxOutputBean(result);
        txOutput.setScriptBytes(new byte[0]);
        txOutput.setValue(Coin.valueOf(rand.nextInt(100)));
        outputs.add(txOutput);
        result.setOutputs(outputs);
        result.makeImmutable();
        return result;

    }

    /** Convenience method to generate a Dummy Tx: */
    public static Tx buildTx() {
        return buildTx(null);
    }

}
