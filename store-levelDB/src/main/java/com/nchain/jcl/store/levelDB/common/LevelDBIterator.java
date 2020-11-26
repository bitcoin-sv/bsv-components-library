package com.nchain.jcl.store.levelDB.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is an Interaor cusotmize to work in the information stored in a LeelDB Database. It leverages on the fact that
 * all the Keys in LevelDB are in lexicograph order. So this Iterator needs a connection to the DB and a "KeyPreffix"
 * starting point: then it will allow to iterate through all the Keys in the Db, as long as those Keys start with the
 * same preffix and we don´´ reach the end of the DB.
 *
 * Additionally, it also accepts a Predicate which is applied of any KEY we loop over, in order to rule out some of
 * them if they not meet the criteria implemented in the Predicate: in that case, the iterator just jumps over to the
 * next.
 */
public abstract class LevelDBIterator<T> implements Iterator<T> {

    private DB levelDB;
    private String keyPreffix;
    private Predicate<String> keyValidPredicate;
    private DBIterator dbIterator;

    // Converts a Key to a Value
    protected byte[] bytes(String value) { return value.getBytes(); }

    // Returns the next Key in the Db, or null if there is no one.
    protected String nextKey() {
        return (dbIterator.hasNext()) ? new String(dbIterator.peekNext().getKey()) : null;
    }

    /**
     * Constructor
     */
    public LevelDBIterator(DB levelDB, String keyPreffix, Predicate<String> keyValidPredicate) {
        // We init the basic properties:
        this.levelDB = levelDB;
        this.keyPreffix = keyPreffix;
        this.keyValidPredicate = keyValidPredicate;

        // We init the DB Iterator and point it to the First Key:
        dbIterator = levelDB.iterator();
        dbIterator.seek(bytes(keyPreffix));
    }
    /** Convenience constructor wihtout predicate to validate the items, so all the items are valid */
    public LevelDBIterator(DB levelDB, String keyPreffix) {
        this(levelDB, keyPreffix, null);
    }

    /**
     * This methods indicates if there is a NEXT Key. Keep in mind that there might be more Keys in the DB, but if
     * those Keys are NOT relevant to us (they don't have the same preffix), or they are relevant but they do NOT meet
     * our filtering criteria (specified by the Predicate whi is apply on every one of them), then the result is FALSE.
     *
     * So this methods returns TRUE if:
     *  - There is next Key in the DB
     *  - The next Key has the same preffix as this Iterator (fed in the constructor)
     *  - The next Key complies with the Predicate (fed in the Constructor). If there is no Predicate specified, the
     *    key is considered valid.
     *
     * I any other case this method returns FALSE.
     */
    @Override
    public boolean hasNext() {
        // We traverse the keys until we find a Key that is Valid
        boolean validKeyFound = false;
        boolean hasMoreKeys = true;
        do {
            String nextKey = nextKey();
            hasMoreKeys = (nextKey != null) && (nextKey.startsWith(keyPreffix));
            validKeyFound = (hasMoreKeys)  &&  (keyValidPredicate == null || keyValidPredicate.test(nextKey));
            if (hasMoreKeys && !validKeyFound) {
                dbIterator.next();
            }

        } while (hasMoreKeys && !validKeyFound);
        return validKeyFound;
    }

    @Override
    public T next() {
        byte[] key = dbIterator.peekNext().getKey();
        byte[] value = dbIterator.peekNext().getValue();
        dbIterator.next();
        return buildItem(key, value);
    }

    /**
     * The Item returned is different depending on the implementation provided by the extending Class. It could be
     * some value/Ovject built based on the Value, or based on the Key itself, or both.
     */
    protected abstract T buildItem(byte[] key, byte[] value);

}
