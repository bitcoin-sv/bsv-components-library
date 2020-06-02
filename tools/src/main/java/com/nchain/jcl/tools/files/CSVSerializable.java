package com.nchain.jcl.tools.files;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-09-17 18:25
 *
 * Classes that implement this interface can be ritten to a TExt file or read from a TExt file in CSV format
 */
public interface CSVSerializable {

    // CSV Line field separator
    String SEPARATOR = ",";

    /**
     * Returns a String representing a line in the CSV file, containing the content of the instance
     */
    String toCSVLine(String separator);
}
