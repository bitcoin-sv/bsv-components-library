package com.nchain.jcl.store.levelDB.common;

import com.nchain.jcl.store.keyValue.common.KeyValueIteratorImpl;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import javax.annotation.Nonnull;
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
public class LevelDBIterator<I> extends KeyValueIteratorImpl<I, Object, Map.Entry<byte[], byte[]>> implements Iterator<I> {

    // Connection to a LevelDB Database:
    private DB levelDB;
    private DBIterator iterator;

    /**
     * Constructor
     */
    public LevelDBIterator(@Nonnull DB database,
                           byte[] startingWithPreffix,
                           byte[] endingWithSuffix,
                           Predicate<byte[]> keyIsValidWhen,
                           Function<Map.Entry<byte[], byte[]>, I> buildItemBy) {
        super(startingWithPreffix, endingWithSuffix,
                (keyIsValidWhen != null)? (tr, key) -> keyIsValidWhen.test(key) : null, buildItemBy);
        try {
            // We init the basic properties:
            this.levelDB = database;

            // We init the Level DB Iterator and point it to the First Key:
            iterator = database.iterator();
            iterator.seek(super.keyPreffix);
        } catch (Exception e) {
            // This mit happens sometimes, when trying to use the Iterator when the Db is closing...
            throw new RuntimeException("Error Initializing LevelDB Iterator");
        }
    }

    @Override protected boolean hasNextItemFromDB()                             { return iterator.hasNext(); }
    @Override protected Map.Entry<byte[], byte[]> nextEntryFromDB()             { return iterator.next(); }
    @Override protected byte[] getKeyFromEntry(Map.Entry<byte[], byte[]> item)  { return item.getKey(); }
    @Override public Object getCurrentTransaction()                             { return null; }

    public static <I> LevelDBIteratorBuilder<I> builder() {
        return new LevelDBIteratorBuilder<I>();
    }

    /**
     * Builder
     * @param <I>
     */
    public static class LevelDBIteratorBuilder<I> {
        private @Nonnull DB database;
        private byte[] startingWithPreffix;
        private byte[] endingWithSuffix;
        private Predicate<byte[]> keyIsValidWhen;
        private Function<Map.Entry<byte[], byte[]>, I> buildItemBy;

        LevelDBIteratorBuilder() {
        }

        public LevelDBIterator.LevelDBIteratorBuilder<I> database(@Nonnull DB database) {
            this.database = database;
            return this;
        }

        public LevelDBIterator.LevelDBIteratorBuilder<I> startingWithPreffix(byte[] startingWithPreffix) {
            this.startingWithPreffix = startingWithPreffix;
            return this;
        }

        public LevelDBIterator.LevelDBIteratorBuilder<I> endingWithSuffix(byte[] endingWithSuffix) {
            this.endingWithSuffix = endingWithSuffix;
            return this;
        }

        public LevelDBIterator.LevelDBIteratorBuilder<I> keyIsValidWhen(Predicate<byte[]> keyIsValidWhen) {
            this.keyIsValidWhen = keyIsValidWhen;
            return this;
        }

        public LevelDBIterator.LevelDBIteratorBuilder<I> buildItemBy(Function<Map.Entry<byte[], byte[]>, I> buildItemBy) {
            this.buildItemBy = buildItemBy;
            return this;
        }

        public LevelDBIterator<I> build() {
            return new LevelDBIterator<I>(database, startingWithPreffix, endingWithSuffix, keyIsValidWhen, buildItemBy);
        }
    }
}
