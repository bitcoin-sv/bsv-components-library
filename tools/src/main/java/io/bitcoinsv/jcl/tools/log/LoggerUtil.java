package io.bitcoinsv.jcl.tools.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An utility class for logging. In most classes, all logs are using the same preffix (like the name of
 * the handler, or the class, or any kind of Id, etc). So in this class you can define that preffix once
 * and it wil be automatically used when logging.
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
            this.preffix = this.preffix + " :: " + groupId;
        }
        logger = LoggerFactory.getLogger(logClass);
    }

    // It generates a single String out of a dynamic list of Objects.
    private String format(Object... objs) {
        StringBuffer result = new StringBuffer(preffix);
        for (Object obj : objs) {
            if (obj != null) {
                result.append(" :: ").append(obj);
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
