package io.bitcoinsv.bsvcl.common.files;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
