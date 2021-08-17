/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.tools.util;

import java.time.*;

/**
 * @author a.vilches@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class DateTimeUtils {

    public static LocalDateTime nowDateTimeUTC() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static long nowMillisUTC() {
        return Instant.now(Clock.systemUTC()).toEpochMilli();
    }

    public static LocalDateTime millisToLocalDateTime(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneOffset.UTC);
    }

    public static LocalDateTime secondsToLocalDateTime(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(milliseconds), ZoneOffset.UTC);
    }

    public static long localDateTimeToMillis(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
