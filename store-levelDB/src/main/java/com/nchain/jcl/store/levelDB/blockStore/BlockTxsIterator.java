package com.nchain.jcl.store.levelDB.blockStore;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.store.levelDB.common.LevelDBIterator;
import org.iq80.leveldb.DB;

import java.util.function.Predicate;

import static com.nchain.jcl.store.levelDB.blockStore.BlockStoreKeyValueUtils.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-11-09
 *
 * A Subclass of LevelDBIterator. This Class assumes that the Keys are related to the "Txs related to one Block",
 * following the schema "btx:[block_hash]:[tx_hash], and returns the "tx_hash" part of the Key as the next Item.
 */
public class BlockTxsIterator extends LevelDBIterator<Sha256Wrapper>  {


    /** constructor */
    public BlockTxsIterator(DB db, String startingKey, Predicate<String> keyValidPredicate) {
        super(db, startingKey, keyValidPredicate);
    }

    /** constructor */
    public BlockTxsIterator(DB db, String startingKey) {
        super(db, startingKey);
    }

    @Override
    public Sha256Wrapper buildItem(byte[] key, byte[] value) {
        String keyStr = new String(key);
        Sha256Wrapper result = Sha256Wrapper.wrap(getTxHashFromKey(keyStr));
        return result;
    }
}
