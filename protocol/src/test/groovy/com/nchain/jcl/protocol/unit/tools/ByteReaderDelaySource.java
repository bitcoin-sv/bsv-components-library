package com.nchain.jcl.protocol.unit.tools;

import com.nchain.jcl.tools.bytes.ByteArrayBuilder;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.streams.InputStreamSourceImpl;
import com.nchain.jcl.tools.streams.StreamCloseEvent;
import com.nchain.jcl.tools.streams.StreamDataEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-15 10:33
 *
 * An implementation of a InutStreamSource that also simulates ral nework activity by adding a Delay
 * when sending bytes down the Stream
 */

@Slf4j
public class ByteReaderDelaySource extends InputStreamSourceImpl<ByteArrayReader> {

    // Number of Bytes that can be send without delay between them
    private int bytesInterval = 5;
    // delay between bytes
    private Duration delay;

    private ByteArrayBuilder byteArrayBuilder;
    private ExecutorService executorService;

    public ByteReaderDelaySource(ExecutorService executor, int bytesPerSec) {
        super(executor);
        // We calculate the Delay:
        this.delay = Duration.ofMillis((bytesInterval * 1000L) / bytesPerSec);

        this.byteArrayBuilder = new ByteArrayBuilder();
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this::feedBytesWithDelay);
    }

    @Override
    public void send(StreamDataEvent<ByteArrayReader> event) {
        byteArrayBuilder.add(event.getData().getFullContent());
    }

    @Override
    public void close(StreamCloseEvent event) {
        try {
            super.close(event);
            this.executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
            this.executorService.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void feedBytesWithDelay() {
        try {
            while (true) {
                // We send some bytes to the Source...
                int bytesToSend = (int) Math.min(byteArrayBuilder.size(), bytesInterval);
                if (bytesToSend > 0) {
                    //log.trace("Feeding " + bytesToSend + " to the Stream...");
                    ByteArrayReader byteReader = new ByteArrayReader(byteArrayBuilder.extractBytes(bytesToSend));
                    super.send(new StreamDataEvent<>(byteReader));

                    // Now we wait...
                    Thread.sleep(delay.toMillis());
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
