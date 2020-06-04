package com.nchain.jcl.tools.files;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-12 16:18
 *
 *
 */
public interface FileUtils {

    /** Returns the Path of the folder where Data is to be stored */
    Path getDataFolder();

    /** Returns the Path of the folder where the Configuration files are stored */
    Path getConfigFolder();

    /** Read the content of the CSV File */
    <T extends CSVSerializable> List<T> readCSV(Path filePath, String separator, Function<String, T> instanceFactory);
    <T extends CSVSerializable> List<T> readCSV(Path filePath, Function<String, T> instanceFactory);

    /** Write the list of items into a CSV File */
    <T extends CSVSerializable> int writeCSV(Path filePath, Stream<T> items, String separator);
    <T extends CSVSerializable> int writeCSV(Path filePath, Collection<T> items, Predicate<T> filter);
    <T extends CSVSerializable> int writeCSV(Path filePath, Collection<T> items);

}
