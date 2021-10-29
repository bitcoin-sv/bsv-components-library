package com.nchain.jcl.net.unit.protocol.tools;


import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayReaderOptimized;
import com.nchain.jcl.tools.bytes.ByteArrayReaderRealTime;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ByteArrayArtificalStreamProducer {

    public static ByteArrayReader stream(byte[] streamedData, int streamDelayByteInterval, int streamDelayTimeMs) {

        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        ByteArrayReader byteArrayReader = new ByteArrayReaderRealTime(byteArrayWriter);
        new Thread(() -> {
            for (int i = 0; i < streamedData.length; i++) {
                //log.trace("Feeding 1 byte...");
                byteArrayWriter.write(streamedData[i]);

                if (streamDelayByteInterval > 0) { // 0 is disabled
                    if (i % streamDelayByteInterval == 0) {
                        try {
                            //log.trace("feeding waiting for " + streamDelayByteInterval + " millisecs ...");
                            Thread.sleep(streamDelayTimeMs);
                        } catch (Exception ex) {
                        }
                    }
                }
            }

        }).start();

        return byteArrayReader;
    }
}