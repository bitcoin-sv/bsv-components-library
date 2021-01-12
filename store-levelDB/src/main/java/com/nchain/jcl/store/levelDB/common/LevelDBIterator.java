package com.nchain.jcl.store.levelDB.common;

import com.nchain.jcl.store.keyValue.common.KeyValueIteratorImpl;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class is an implementation of an Iterator that fetches the data from a LevelDB DataBase.
 * It extends the functionality of KeyValueIteratorIml, and provides specific implementations for those methods that
 * need to be rewritten, since they depen on specific implemenation-details.
 */
@Slf4j
public class LevelDBIterator<I> extends KeyValueIteratorImpl<I, Object, Map.Entry<byte[], byte[]>> implements Iterator<I> {

    // Connection to a LevelDB Database:
    private DB levelDB;
    private DBIterator iterator;

    /**
     * Constructor
     */
    @Builder
    public LevelDBIterator(@NonNull DB database,
                           byte[] startingWithPreffix,
                           byte[] endingWithSuffix,
                           Predicate<byte[]> keyIsValidWhen,
                           Function<Map.Entry<byte[], byte[]>, I> buildItemBy) {

        super(startingWithPreffix, endingWithSuffix,
              (keyIsValidWhen != null)? (tr, key) -> keyIsValidWhen.test(key) : null, buildItemBy);

        // We init the basic properties:
        this.levelDB = database;

        // We init the Level DB Iterator and point it to the First Key:
        iterator = database.iterator();
        iterator.seek(super.keyPreffix);
    }

    @Override protected boolean hasNextItemFromDB()                             { return iterator.hasNext(); }
    @Override protected Map.Entry<byte[], byte[]> nextEntryFromDB()             { return iterator.next(); }
    @Override protected byte[] getKeyFromEntry(Map.Entry<byte[], byte[]> item)  { return item.getKey(); }
    @Override public Object getCurrentTransaction()                             { return null; }
}
