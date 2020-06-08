package com.nchain.jcl.protocol.unit.tools;

import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;


public class ByteArrayArtificalStreamProducer {

    public static ByteArrayReader stream(byte[] streamedData, int streamDelayByteInterval, int streamDelayTimeMs) {

        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        ByteArrayReader byteArrayReader = new ByteArrayReader(byteArrayWriter);

        new Thread(() -> {
            for (int i = 0; i < streamedData.length; i++) {
                byteArrayWriter.write(streamedData[i]);

                if (streamDelayByteInterval > 0) { // 0 is disabled
                    if (i % streamDelayByteInterval == 0) {
                        try {
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
