package io.bitcoinsv.jcl.net.network.streams.nio;

import io.bitcoinsv.jcl.net.network.streams.PeerStreamer;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.writebuffer.WriteBuffer;

import java.io.IOException;

public class NIOStreamer implements PeerStreamer<ByteArrayReader> {
    // Here we keep the bytes pending to be written to the Socket:
    private final WriteBuffer writeBuffer;

    public NIOStreamer(WriteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    @Override
    public void send(ByteArrayReader data) {
        writeBuffer.write(data);
    }

    @Override
    public void close() {
        writeBuffer.stop();
    }

    public synchronized int extract() throws IOException {
        return writeBuffer.extract();
    }
}
