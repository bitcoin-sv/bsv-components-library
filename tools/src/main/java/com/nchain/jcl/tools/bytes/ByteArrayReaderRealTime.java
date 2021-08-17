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

    /**
     * This reader will wait for the bytes its reading of those bytes are not available yet. But they way it waits is
     * determined by this mode:
     */
    enum ReaderMode {
        FIXED_WAIT,     // it waits always the same time, regardless of the amount of bytes we want to read
        DYNAMIC_WAIT    // it waits an amount of time which is based on the speed (bytes/Sec) we expect for this reader
    }

    // during the wait, we wait several times for small amounts of time each, specified in this property:
    private static final Duration WAITING_INTERVAL = Duration.ofMillis(50);

    // If we are in FIXED_WAIT mode, the waitingTime will take this value for the next bytes to read (no matter how many bytes)
    private static final Duration FIXED_WAIT_TIMEOUT = Duration.ofMillis(5000); // 5 sec

    // In DYNAMIC_MODE, this is the default speed that will determine the waitingTime
    // NOTE: This is a VERY LOW SPEED, but in practice some Peers seems to get idle sometimes and then they burst into
    // sending data like crazy. So this speed is just something we use so we don´´ run into a deadlock waiting for
    // bytes...
    public static final int DEFAULT_SPEED_BYTES_PER_SECOND = 10; // 10 bytes/sec

    // Current Speed expected by this reader when in DYNAMIC_MODE:
    private int speedBytesPerSec = DEFAULT_SPEED_BYTES_PER_SECOND;

    // Reader Mode:
    private ReaderMode readerMode = ReaderMode.FIXED_WAIT;

    private static final Logger log = LoggerFactory.getLogger(ByteArrayReaderRealTime.class);

    // for statistics: Total time this Byte read has been waiting for bytes to arrive...
    protected Duration waitingTime = Duration.ZERO;

    public ByteArrayReaderRealTime(ByteArray byteArray, int speedBytesPerSec) {
        super(byteArray);
        this.speedBytesPerSec = speedBytesPerSec;
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

    private Duration getWaitingTime(long length) {
        return readerMode.equals(ReaderMode.FIXED_WAIT)
                ? FIXED_WAIT_TIMEOUT
                : Duration.ofMillis(length * 1000 / speedBytesPerSec);
    }

    public byte[] read(int length) {
        waitForBytes(length);
        return super.read(length);
    }

    public byte[] get(int offset, int length) {
        waitForBytes(offset + length);
        return super.get(offset, length);
    }

    public byte[] get(int length) {
        waitForBytes(length);
        return super.get(length);
    }

    public long getUint32(int offset) {
        waitForBytes(offset + 4);
        return super.getUint32(offset);
    }

    public long readUint32() {
        waitForBytes(4);
        return super.readUint32();
    }

    public byte read() {
        waitForBytes(1);
        return super.read();
    }

    public long getInt64LE(int offset) {
        waitForBytes(offset + 8);
        return super.getInt64LE(offset);
    }

    public long readInt64LE() {
        waitForBytes(8);
        return super.readInt64LE();
    }

    public boolean readBoolean() {
        return (read() != 0);
    }

    public void updateReaderSpeed(int speedBytesPerSec) {
        this.readerMode = ReaderMode.DYNAMIC_WAIT;
        this.speedBytesPerSec = speedBytesPerSec;
    }

    public void resetReaderSpeed() {
        this.readerMode = ReaderMode.FIXED_WAIT;
        this.speedBytesPerSec = DEFAULT_SPEED_BYTES_PER_SECOND;
    }

    /*
     * Waits for the bytes to be written before returning. This will cause the thread to be blocked.
     */
    public void waitForBytes(int length) throws RuntimeException {
        long millisecsToWait = getWaitingTime(length).toMillis();

        long timeout = System.currentTimeMillis() + millisecsToWait;

        while (size() < length) {

            if (System.currentTimeMillis() > timeout) {
                String errorLine = "timeout waiting longer than " + millisecsToWait + " millisecs for " + length + " bytes, current size: " + size();
                if (readerMode.equals(ReaderMode.DYNAMIC_WAIT)) {
                    errorLine += " minSpeed = " + speedBytesPerSec + " bytes/sec";
                }
                throw new RuntimeException(errorLine);
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
