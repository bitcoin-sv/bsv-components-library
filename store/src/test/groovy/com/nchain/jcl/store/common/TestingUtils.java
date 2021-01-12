package com.nchain.jcl.store.common;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.*;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.blockStore.BlockStore;

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
        return HEX.encode(bytes);
    }

    /** Convenience method to generate a Dummy Block */
    public static BlockHeader buildBlock(String parentHashHex) {
        Sha256Wrapper parentHashFinal = (parentHashHex != null) ? Sha256Wrapper.wrap(parentHashHex) : Sha256Wrapper.ZERO_HASH;
        return BlockHeader.builder()
               // .hash(Sha256Wrapper.wrap(blockHashHex))
                .merkleRoot(Sha256Wrapper.wrap(buildRandomHash()))
                .prevBlockHash(parentHashFinal)
                .nonce(1)
                .difficultyTarget(10)
                .time(Instant.now().getEpochSecond())
                .build();
    }

    /** Convenience method to build a Block with NO Parent */
    public static BlockHeader buildBlock() {
        return buildBlock(buildRandomHash());
    }

    /** Convenience method to generate a Dummy Tx, specifying a parent Tx */
    public static Tx buildTx(String parentTxHash) {
        Random rand = new Random();
        // Dummy Tx with one input and one Output:
        // If "parentTxHash" is not null, we use it as the Hash of the TX which output we are spending
        String parentOutputHash = (parentTxHash != null) ? parentTxHash : buildRandomHash();

        return Tx.builder()
                .version(1)
                .lockTime(2)
                .inputs(Arrays.asList(
                        TxInput.builder()
                                .sequenceNumber(rand.nextInt(100))
                                .scriptBytes(new byte[0])
                                .outpoint(
                                        TxOutPoint.builder()
                                                .index(rand.nextInt(10))
                                                .hash(Sha256Wrapper.wrap(parentOutputHash))
                                                .build())
                                .build()))
                .outputs(Arrays.asList(
                        TxOutput.builder()
                                .scriptBytes(new byte[0])
                                .value(Coin.valueOf(rand.nextInt(100)))
                                .build()
                ))
                .build();
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
    public static long performanceLinkBlockAndTxs(BlockStore db, BlockHeader block, int numTxs) {
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
    public static long performanceSaveBlockAndTxs(BlockStore db, BlockHeader block, int numTxs) {
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
