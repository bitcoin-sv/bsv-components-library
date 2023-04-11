package io.bitcoinsv.bsvcl.net.tools;


import com.google.common.base.Strings;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An utility class for logging the NET module in JCL. its only meant for improving the log readability,
 * so it just formats the text in such a way that it easier to read.
 *
 * NOTE: The operations performanced in this class might affect performance, since they all involve operations
 * with Strings. If the log level is high enough in a prouction environment this will not be a problem, but in
 * DEBUG or TRACE Mode the performance impact should be evaluated.
 */
public class LoggerUtil {

    // 2 different preffixes can be used: instanceIf and groupID:
    private String instanceId;
    private String groupId;

    // Preffix that will be append to the beginning of every log. Its pre-calculated at instance creation:
    private String preffix;

    // the class the log will be linked to
    private Class logClass;

    // The real Logger used behind the scenes:
    private Logger logger;

    // Constructor, only instanceIf is specified
    public LoggerUtil(String instanceId, Class logClass) {
        this(instanceId, null, logClass);
    }

    // Constructor
    public LoggerUtil(String instanceId, String groupId, Class logClass) {
        this.instanceId = instanceId;
        this.groupId = groupId;
        this.logClass = logClass;
        this.preffix = instanceId;

         if (groupId != null) {
          this.preffix = this.preffix + " :: " + Strings.padEnd(groupId, 14, ' ');
        }
        logger = LoggerFactory.getLogger(logClass);
    }

    /**
     * It returns a new instance of this logger, using the oone given as a template, but replacing the 'groupId' and
     * the 'logClass' (but keeping the 'instanceId').
     */
    public static LoggerUtil of(LoggerUtil parentLogger, String groupId, Class logClass) {
        return new LoggerUtil(parentLogger.instanceId, groupId, logClass);
    }

    // It generates a single String out of a dynamic list of Objects.
    private String format(Object... objs) {
        StringBuffer result = new StringBuffer(preffix);
        for (Object obj : objs) {
            if (obj != null) {
                result
                   .append(" :: ")
                   .append((obj instanceof PeerAddress || obj instanceof InetAddress)
                           ? Strings.padEnd(obj.toString(), 36, ' ')
                           : obj);
            }
        }
        return result.toString();
    }

    public void trace(Object... args)                   { logger.trace(format(args)); }
    public void debug(Object... args)                   { logger.debug(format(args)); }
    public void info(Object... args)                    { logger.info(format(args)); }
    public void warm(Object... args)                    { logger.warn(format(args)); }
    public void error(Object... args)                   { logger.error(format(args), (Throwable) null); }
    public void error(Throwable th, Object... args)     { logger.error(format(args), th); }

}
