package com.nchain.jcl.tools.files;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 10:39
 *
 * An interface with useful methods to work with files and Folders. It uses a "rootPath" which is the "root" folder
 * that wil be used to write or find/read files from.
 */
public interface FileUtils {
    /** Return the Root Path in the File System */
    Path getRootPath();

    /**
     * Reads a CSV file and returns a List of instances. Each Instance is created using the "instanceFactory" supplied,
     * and each of them must implement the CSVSerializable interface.
     */
    <T extends CSVSerializable> List<T> readCV(Path pathFile, Supplier<T> instanceFactory);

    /**
     * It writes a CSV file containing the info related to the Colleciton of Objects gien. Each object must implement
     * the CSVSerializable interface.
     */
    <T extends CSVSerializable> int writeCSV(Path pathFile, Collection<T> items);
}
