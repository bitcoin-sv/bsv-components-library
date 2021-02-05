package com.nchain.jcl.store.common;


import com.nchain.jcl.store.blockStore.BlockStore;
import io.bitcoinj.bitcoin.api.base.*;
import io.bitcoinj.bitcoin.bean.base.*;
import io.bitcoinj.core.Coin;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


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
        return buildBlock(buildRandomHash());
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

    // To save memory, we only saved Txs in batches of this size:
    public static final int BATCH_TXS_SIZE = 10_000;

    /**
     * It saves a Block and a variable number of Txs eparately, and then they linke them together, returning the time the whole operation takes
     */
    public static long performanceLinkBlockAndTxs(BlockStore db, HeaderReadOnly block, int numTxs) {
        Instant beginTime = Instant.now();
        db.saveBlock(block);
        System.out.println(" - Block saved.");
        System.out.println(" - Linking Txs to the Block...");
        List<Tx> txsToInsert = new ArrayList<>();
        for (int i = 0; i < numTxs; i++) {
            txsToInsert.add(TestingUtils.buildTx());
            if ((txsToInsert.size() == BATCH_TXS_SIZE) || (i == (numTxs -1))) {
                db.saveTxs(txsToInsert);
                System.out.println(" - " + txsToInsert.size() + " Txs saved, " + (i + 1) + " in total.");
                db.linkTxsToBlock(txsToInsert.stream().map(tx -> tx.getHash()).collect(Collectors.toList()), block.getHash());
                System.out.println(" - " + txsToInsert.size() + " Txs linked to the Block, " + (i + 1) + " in total.");
                txsToInsert.clear();
            }
        } // for...
        long result = Duration.between(beginTime, Instant.now()).toMillis();
        return result;
    }

    /**
     * It saves a Block and a variable number of Txs and link them together at the same time
     */
    public static long performanceSaveBlockAndTxs(BlockStore db, HeaderReadOnly block, int numTxs) {
        Instant beginTime = Instant.now();
        db.saveBlock(block);
        System.out.println(" - Block saved.");
        System.out.println(" - Linking Txs to the Block...");
        List<Tx> txsToInsert = new ArrayList<>();
        for (int i = 0; i < numTxs; i++) {
            txsToInsert.add(TestingUtils.buildTx());
            if ((txsToInsert.size() == BATCH_TXS_SIZE) || (i == (numTxs -1))) {
                db.saveBlockTxs(block.getHash(), txsToInsert);
                System.out.println(" - " + txsToInsert.size() + " Txs saved and linked to the Block , " + (i + 1) + " in total.");
                txsToInsert.clear();
            }
        } // for...
        long result = Duration.between(beginTime, Instant.now()).toMillis();
        return result;
    }
}
