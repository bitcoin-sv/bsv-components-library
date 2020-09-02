package com.nchain.jcl.base.tools.files;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-25 10:40
 *
 * This interfaces provides 2 methods useful for moving information from an object instance to a String line following
 * the CSV format. An object impleting this interface wil be able to save its info into a CSV fle and load it from it.
 */
public interface CSVSerializable {
    // Separator used in the CSV file
    String SEPARATOR = ",";

    /**
     * Returns a line (String) containing the object information. different fields will be separated using SEPARATOR
     */
    String toCSVLine();

    /**
     * It loads the object with the information obtained from the line given. different fields are separated using
     * SEPARATOR
     */
    void fromCSVLine(String line);
}
