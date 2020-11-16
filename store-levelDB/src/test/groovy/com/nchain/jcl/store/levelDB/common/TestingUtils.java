package com.nchain.jcl.store.levelDB.common;

import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.*;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Utility Class with useful methods for Testing
 */
public class TestingUtils {

    // Folder to store the LevelDB files in:
    private static final String DB_TEST_FOLDER = "testingDB";

    // Convenience method to generate a Random Folder to use as a working Folder:
    public static String buildWorkingFolder() {
        return DB_TEST_FOLDER + "/test-" + new Random().nextInt(100) + new Random().nextInt(100);
    }

    // Convenience method to generate a (Random) Hash
    public static String buildRandomHash() {
        byte[] bytes = new byte[32];
        Random rand = new Random();
        for (int i = 0; i < 32 ; i++) bytes[i] = (byte) rand.nextInt(255);
        return HEX.encode(bytes);
    }

    // Convenience method to generate a Dummy Block
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

    // Convenience method to build a Block with NO Parent
    public static BlockHeader buildBlock() {
        return buildBlock(buildRandomHash());
    }

    // Convenience method to generate a Dummy Tx, specifying a parent Tx
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

    // Convenience method to generate a Dummy Tx:
    public static Tx buildTx() {
        return buildTx(null);
    }
}
