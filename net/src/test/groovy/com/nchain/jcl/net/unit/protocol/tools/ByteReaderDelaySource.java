package com.nchain.jcl.net.unit.protocol.tools;


import com.nchain.jcl.net.network.streams.PeerInputStreamImpl;
import com.nchain.jcl.net.network.streams.StreamCloseEvent;
import com.nchain.jcl.net.network.streams.StreamDataEvent;
import com.nchain.jcl.net.unit.network.streams.PeerStreamInOutSimulator;
import com.nchain.jcl.tools.bytes.ByteArrayBuilder;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
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
 * An implementation of a InputStreamSource that also simulates ral nework activity by adding a Delay
 * when sending bytes down the Stream
 */

@Slf4j
public class ByteReaderDelaySource extends PeerStreamInOutSimulator<ByteArrayReader> {

    // Speed
    final int bytesPerSec;
    // bytesBatchSize: This is the minimum number of bytes that can be feed in a single go. The delay must be applied
    // between different batches. If the size of the buffer is less than this, no delay is applied.
    final int bytesBatchsize = 10;

    private ByteArrayBuilder byteArrayBuilder;
    private ExecutorService executorService;

    public ByteReaderDelaySource(ExecutorService executor, int bytesPerSec) {
        super(null, executor); // no PeerAddress
        this.bytesPerSec = bytesPerSec;

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
                long bufferSize = byteArrayBuilder.size();
                if (byteArrayBuilder.size() > 0) {
                    // the time that will take to send the whole buffer...
                    long millisecsWholeSend = (long) ((byteArrayBuilder.size() / bytesPerSec) * 1000);

                    int numBatches = ((int) bufferSize / bytesBatchsize) + 1;

                    // The delay between each bytes:
                    Duration delay = Duration.ofMillis(millisecsWholeSend / numBatches);

                    int bytesToSend = (int) Math.min(byteArrayBuilder.size(), bytesBatchsize);
                    ByteArrayReader byteReader = new ByteArrayReader(byteArrayBuilder.extractBytes(bytesToSend));
                    super.send(new StreamDataEvent<>(byteReader));

                    // Now we wait...
                    Thread.sleep(delay.toMillis());
                } else Thread.sleep(500); // We wait until the buffer is fed with some bytes...

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
