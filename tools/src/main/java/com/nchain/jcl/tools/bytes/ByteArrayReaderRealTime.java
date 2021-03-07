package com.nchain.jcl.tools.bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * This class is a further specialization. On top of the optimizations already implemented by ByteArrayOptimizsed,
 * this class adds a "real-time" feature: the operations to read the data WAIT until the data is available. So this
 * class can be used to read data from the ByteArray even If the data is still not there.
 * This is usefull is some situations where we need to read BIG chunks of data and deserialize them: In a regular
 * scenario, we jusrt wait intil all the bytes are available in the ByteArray, and then we use a ByteReader to
 * deserialize them. this might not work for Big ByteArrays, sincde we might risk triggering an OutOfMemory Error. In
 * these specific cases, this class can be used: From the very moment we detect that a HUghe message is coming, we can
 * start READING from it right away: the bytes will be read, and this class will wait if they are not htere yet.
 * <p>
 *
 * NOTE: There is alimit to how long we are willing to wait for the info to be available, and if the timeout is broken
 * then an error is thrown.
 *
 */
public class ByteArrayReaderRealTime extends ByteArrayReaderOptimized {

    protected static final Duration THRESHOLD_WAITING = Duration.ofMillis(5000); // 5 sec
    protected static final Duration WAITING_INTERVAL = Duration.ofMillis(100);

    private static final Logger log = LoggerFactory.getLogger(ByteArrayReaderRealTime.class);

    // Total time this Byte read has been waiting for bytes to arrive...
    protected Duration waitingTime = Duration.ZERO;
    protected Duration thresholdWaiting = THRESHOLD_WAITING;

    public ByteArrayReaderRealTime(ByteArray byteArray, Duration thresholdWaiting) {
        super(byteArray);
        this.thresholdWaiting = thresholdWaiting;
    }

    public ByteArrayReaderRealTime(ByteArray byteArray) {
        super(byteArray);
    }

    public ByteArrayReaderRealTime(ByteArrayReader reader) {
        super(reader);
    }

    public ByteArrayReaderRealTime(ByteArrayWriter writer) {
        super(writer);
    }

    public ByteArrayReaderRealTime(byte[] initialData) {
        super(initialData);
    }

    public byte[] read(int length) {
        waitForBytes(length);
        return super.read(length);
    }

    public byte[] get(int length) {
        waitForBytes(length);
        return super.get(length);
    }

    public long readUint32() {
        waitForBytes(4);
        return super.readUint32();
    }

    public byte read() {
        waitForBytes(1);
        return super.read();
    }

    public long readInt64LE() {
        waitForBytes(8);
        return super.readInt64LE();
    }

    public boolean readBoolean() {
        return (read() != 0);
    }

    /*
     * Waits for the bytes to be written before returning. This will cause the thread to be blocked.
     */
    public void waitForBytes(int length) throws RuntimeException {

        long timeout = System.currentTimeMillis() + thresholdWaiting.toMillis();

        while (size() < length) {

            if (System.currentTimeMillis() > timeout) {
                throw new RuntimeException("timed out waiting longer than " + thresholdWaiting.toMillis() + " millisecs for " + length + " bytes");
            }

            try {
                //log.trace("waiting for " + (millisecsToWait) + " millisecs to get " + length + " bytes, byteArray Size: " + byteArray.size());
                Thread.sleep(WAITING_INTERVAL.toMillis());
                waitingTime = waitingTime.plus(WAITING_INTERVAL);
            } catch (InterruptedException ex) {}
        }
        //log.trace("WAit finish, bufferSize: " + byteArray.size());
        return;
    }
}
