package com.nchain.jcl.tools.log;

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
        logger = LoggerFactory.getLogger(logClass);
    }

    // It generates a single String out of a dynamic list of Objects.
    private String format(Object... objs) {
        StringBuffer result = new StringBuffer();
        result.append(instanceId);
        if (groupId != null) result.append(" :: ").append(groupId);
        if (objs.length > 0)
            for (Object obj : objs) if (obj != null) result.append(" :: ").append(obj.toString());
        return result.toString();
    }

    private void log(Level level, Throwable th,  Object... objs) {
        switch (level) {
            case DEBUG: {
                logger.debug(format(objs));
                break;
            }
            case INFO: {
                logger.info(format(objs));
                break;
            }
            case WARN: {
                logger.warn(format(objs));
                break;
            }
            case TRACE: {
                logger.trace(format(objs));
                break;
            }
            case ERROR: {
                logger.error(format(objs), th);
                break;
            }
        } // switch
    }

    public void trace(Object... args)                   { log(Level.TRACE, null, args); }
    public void debug(Object... args)                   { log(Level.DEBUG, null, args); }
    public void info(Object... args)                    { log(Level.INFO, null, args); }
    public void warm(Object... args)                    { log(Level.WARN, null, args); }
    public void error(Object... args)                   { log(Level.ERROR, null, args); }
    public void error(Throwable th, Object... args)     { log(Level.ERROR, th, args); }


}
