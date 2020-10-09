package com.nchain.jcl.base.tools.bytes;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This class allows for reading data out of a ByteArray, and automatically converts the data into useful
 * representations, like unsigned Integers, etc.
 * <p>
 * IMPORTANT: The Reader CONSUMES the data as it reads them, so its not possible to use the same Reader twice.
 */
@Slf4j
public class ByteArrayReader {

    protected static final Duration THRESHOLD_WAITING = Duration.ofMillis(5000); // 5 sec
    protected static final Duration WAITING_INTERVAL = Duration.ofMillis(100);

    protected ByteArrayBuilder builder;
    protected long bytesReadCount = 0; // Number of bytes read....
    protected boolean realTimeProcessingEnabled;
    // Total time this Byte read has been waiting for bytes to arrive...
    protected Duration waitingTime = Duration.ZERO;
    protected Duration thresholdWaiting = THRESHOLD_WAITING;


    public ByteArrayReader(ByteArrayBuilder builder)    { this(builder, null, false); }
    public ByteArrayReader(ByteArrayWriter writer)      { this(writer.builder, null, false);}

    public ByteArrayReader(byte[] initialData) {
        this(new ByteArrayBuilder(), initialData, false);
    }

    public ByteArrayReader(byte[] initialData, boolean waitingForBytes) {
        this(new ByteArrayBuilder(), initialData, waitingForBytes);
    }

    public ByteArrayReader(ByteArrayBuilder builder, byte[] initialData, boolean waitForBytes) {
        this(builder, initialData, waitForBytes, THRESHOLD_WAITING);
    }

    public ByteArrayReader(ByteArrayBuilder builder, byte[] initialData, boolean waitForBytes, Duration thresholdWaiting) {
        this.builder = builder;
        if (waitForBytes) {
            enableRealTime(thresholdWaiting);
        }
        if (initialData != null) this.builder.add(initialData);
    }

    public byte[] read(int length) {
        byte[] result = builder.extractBytes(length);
        bytesReadCount += length;
        return result;
    }

    public byte[] extract(int length)       {
        bytesReadCount += length;
        return builder.extractBytes(length);
    }
    public byte[] get(int length)           { return builder.get(length);}
    public long readUint32()                { return ByteTools.readUint32(read(4)); }
    public byte read()                      { return read(1)[0]; }
    public long readInt64LE()               { return ByteTools.readInt64LE(read(8)); }
    public boolean readBoolean()            { return (read() != 0);}
    public long size()                      { return builder.size();}
    public boolean isEmpty()                { return builder.size() == 0;}
    public void close()                     { builder.clear();}
    public byte[] getFullContent()          { return builder.getFullContent();}
    public long getBytesReadCount()         { return bytesReadCount;}

    public byte[] getFullContentAndClose() {
        byte[] result = builder.getFullContent();
        close();
        bytesReadCount += result.length;
        return result;
    }

    public String readString(int length, String charset) {
        try {
            String result = new String(read(length), charset);
            return result.trim();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void enableRealTime() {
        enableRealTime(thresholdWaiting);
    }

    public void enableRealTime(Duration thresholdWaiting) {
        this.realTimeProcessingEnabled = true;
        this.thresholdWaiting = thresholdWaiting;
    }

    public void disableRealTime() {
        this.realTimeProcessingEnabled = false;
    }

    /*
     * Waits for the bytes to be written before returning. This will cause the thread to be blocked.
     */
    public void waitForBytes(int length) throws RuntimeException {

        long timeout = System.currentTimeMillis() + thresholdWaiting.toMillis();


        while (size() < length) {
            if (!realTimeProcessingEnabled) throw new RuntimeException("Not enough bytes to read");
            else if (System.currentTimeMillis() > timeout) {
                throw new RuntimeException("timed out waiting longer than " + thresholdWaiting.toMillis() + " millisecs for " + length + " bytes");
            }

            try {
                //log.trace("waiting for " + (millisecsToWait) + " millisecs to get " + length + " bytes, builder Size: " + builder.size());
                Thread.sleep(WAITING_INTERVAL.toMillis());
                waitingTime = waitingTime.plus(WAITING_INTERVAL);
            } catch (InterruptedException ex) {}
        }
        //log.trace("WAit finish, bufferSize: " + builder.size());
        return;
    }
}
