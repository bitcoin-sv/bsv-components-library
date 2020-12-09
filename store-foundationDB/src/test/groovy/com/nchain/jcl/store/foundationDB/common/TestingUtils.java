package com.nchain.jcl.store.foundationDB.common;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.*;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Utility Class with useful methods for Testing
 */
public class TestingUtils {


    // Clears the whole DB (Only for Testing)
    public static void clearDB(Database db) {
        db.run(tr -> {
            try {
                 tr.clear("".getBytes(), "\\xff".getBytes());
                 // We remove al the blocks-Subfolders:
                 DirectoryLayer dirLayer = new DirectoryLayer();
                 DirectorySubspace blocksDir = dirLayer.open(tr, Arrays.asList(KeyValueUtils.DIR_BLOCKCHAIN, "BSV-Mainnet", KeyValueUtils.DIR_BLOCKS)).get();
                 List<String> subDirs = blocksDir.list(tr).get();
                 subDirs.forEach(d -> {
                     try {
                         System.out.println("removing subfolder \\" + d + "...");
                         blocksDir.remove(tr, Arrays.asList(d)).get();
                     } catch (InterruptedException | ExecutionException e) {
                         throw new RuntimeException(e);
                     }
                 });
                 //tr.commit().get();
                 return null;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
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
