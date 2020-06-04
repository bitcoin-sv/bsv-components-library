package com.nchain.jcl.tools.loggin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-27 03:25
 *
 * A Base class with utility methods to format the log.
 * The log produces by this project might be huge, so formatting and applying a layout ti it
 * might make it easier to read. This class provides methods to format String to an specific length
 * and also to separate different String by specific separators, so reading the log on a Terminal or
 * on a file is easier.
 */
public class FormatLogger {

    // Separator used between different messages
    public static final String DEFAULT_SEPARATOR = " : ";

    // We are using SLF4J behind the scenes, so we need the reference to
    // the source class so that modifying the appenders in the configuration file works as expected.

    private Class logClass;
    protected Logger logger;

    /** Constructor */
    public FormatLogger(Class logClass) {
        this.logClass = logClass;
        this.logger = LoggerFactory.getLogger(logClass);
    }

    /**
     * Formats a String so it takes the exact number of characters. If its lenght exceeds the
     * maxLength, it's truncated, otherwise is filled with blanks.
     */
    protected String format(String value, int maxLength) {
        return (value.length() > maxLength)
                ? value.substring(0, maxLength - 3) + "..."
                : value + Stream.generate(() -> " ").limit(maxLength - value.length()).collect(Collectors.joining());
    }

    /**
     * Formats a series of Messages, returning a single String joining all of them by a separator.
     */
    protected String format(String ...msgs) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < msgs.length; i++) {
            if (i > 0) str.append(DEFAULT_SEPARATOR);
            str.append(msgs[i]);
        }
        return str.toString();
    }
}
