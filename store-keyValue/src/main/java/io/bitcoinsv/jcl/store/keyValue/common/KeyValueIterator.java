package io.bitcoinsv.jcl.store.keyValue.common;

import java.util.Iterator;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * The iterators used in the JCL-Store modules behave like regular "Iterators", but they also provide additional
 * capacities, which might be useful in some scenarios. These additional capabilities are usually related to some
 * DB features that might be implementation-specific.
 *
 * This interface is parameterized:
 *
 * @param <I> Type returned by the "next()" method. It represents each one of the TYPES returned by the iterator
 * @param <T> Type of the DB Transaction supported by the specific implementation. If the specific implementation does
 *            Not support Transactions, this can be just an "Object".
 */
public interface KeyValueIterator<I,T> extends Iterator<I> {

    /**
     * This methods returns the current DB Transaction this iterator is running with.
     * ALL Iterators returned by the JCL-Store Modules take their data form the DB, so that means that all of them are
     * running within a Transaction. In some scenarios, it might be useful to get the Current Transaction this Iterator
     * is running with.
     *
     * For example, some DB-specific implementations of this Iterator might make changes on the current Transaction,
     * or even closeAndClear it and open a new one if the Iterator is iterating over a huge number of items, to prevent the
     * Transaction to expire. In those cases, it's useful to get a way to obtain a reference to the current Transaction
     * of the iterator, since it might Not be the same as the one used when the Iterator was created in the first place.
     *
     * @return  the current Transaction
     */
    T getCurrentTransaction();
}
