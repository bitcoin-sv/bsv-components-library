package com.nchain.jcl.tools.handlers.common;



import com.nchain.jcl.tools.loggin.FormatLogger;
import org.slf4j.event.Level;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-26 16:07
 *
 * An utility class for logging the output of a NET Handler
 */
public class HandlerLogger extends FormatLogger {

    private static final int ID_MAX_LENGTH = 10;
    private static final int HANDLER_NAME_MAX_LENGTH = 25;
    private static final int PEER_MAX_LENGTH = 25;

    private String connId;
    private String handlerName;

    private String connIdFormatted;
    private String handlerNameFormatted;

    /** Constructor */
    public HandlerLogger(Class logClass, String connId, String handlerName) {
        super(logClass);
        this.connId = connId;
        this.handlerName = handlerName;

        this.connIdFormatted = format(connId, ID_MAX_LENGTH);
        this.handlerNameFormatted = format(handlerName, HANDLER_NAME_MAX_LENGTH);

    }

    //  Methods to log Handler messages:

    public void log(Level level, Throwable th,  String ...msgs) {
        switch (level) {
            case DEBUG: {
                logger.debug(format(connIdFormatted, handlerNameFormatted, format(msgs)));
                break;
            }
            case INFO: {
                logger.info(format(connIdFormatted, handlerNameFormatted, format(msgs)));
                break;
            }
            case WARN: {
                logger.warn(format(connIdFormatted, handlerNameFormatted, format(msgs)));
                break;
            }
            case TRACE: {
                logger.trace(format(connIdFormatted, handlerNameFormatted, format(msgs)));
                break;
            }
            case ERROR: {
                logger.error(format(connIdFormatted, handlerNameFormatted, format(msgs)), th);
                break;
            }
        } // switch
    }

    public void log(Level level, String ...msgs) {
        log(level, null, msgs);
    }

    public void logPeer(Level level, String peerAddress, String ...msgs) {
        log(level, format(peerAddress, PEER_MAX_LENGTH), format (msgs));
    }

}
