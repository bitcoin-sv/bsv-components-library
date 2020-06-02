package com.nchain.jcl.protocol.serialization;



import com.nchain.jcl.network.PeerAddress;
import com.nchain.jcl.protocol.config.ProtocolVersion;
import com.nchain.jcl.protocol.messages.NetAddressMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-15
 *
 * A Serializer for  {@link NetAddressMsg} messages
 */
public class NetAddressMsgSerializer implements MessageSerializer<NetAddressMsg> {

    private static NetAddressMsgSerializer instance;

    // Constructor
    private NetAddressMsgSerializer() { }

    /** Returns the instance of this Class (Singleton) */
    public static NetAddressMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (NetAddressMsgSerializer.class) {
                instance = new NetAddressMsgSerializer();
            }
        }
        return instance;
    }


    @Override
    public NetAddressMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            long timestamp= 0;

            // if applied, we read the timestamp. 4 bytes
            if  ((!context.isInsideVersionMsg()) &&
                    (context.getProtocolconfig().getHandshakeProtocolVersion() >= ProtocolVersion.ENABLE_TIMESTAMP_ADDRESS.getBitcoinProtocolVersion())) {
                timestamp  = byteReader.readUint32();
            }

            long services = byteReader.readInt64LE();

            // We now process the Address, which consists of the IP first, and the PORT after that.
            // We read the "Address" field. 16 bytes
            byte[] addrBytes = byteReader.read(16);
            InetAddress inetAddr = InetAddress.getByAddress(addrBytes);

            // We read the "Port" field. 2 bytes, network byte order
            byte partialPort1 = byteReader.read();
            byte partialPort2 = byteReader.read();
            int port = ((0xFF & partialPort1) << 8) | (0xFF & partialPort2);

            NetAddressMsg netAddressMsg = NetAddressMsg.builder().timestamp(timestamp).services(services).address(new PeerAddress(inetAddr, port)).build();
            return netAddressMsg;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Error during serialization", e);
        }
    }

    @Override
    public void serialize(SerializerContext context, NetAddressMsg message, ByteArrayWriter byteWriter) {
        // if applied, we write the timestamp. 4 bytes
        if  ((!context.isInsideVersionMsg()) &&
                (context.getProtocolconfig().getHandshakeProtocolVersion() >= ProtocolVersion.ENABLE_TIMESTAMP_ADDRESS.getBitcoinProtocolVersion())) {
            byteWriter.writeUint32LE(message.getTimestamp());
        }

        // We write the "services" field. Long 8 Bytes
        byteWriter.writeUint64LE(message.getServices());

        // We write the "Address" field. 16 bytes
        // If the InetAddress formats is IPv4, we write it s a 16 byte IPv4-mapped IPv6 address
        // (12 bytes 00 00 00 00 00 00 00 00 00 00 FF FF, followed by the 4 bytes of the IPv4 address).

        byte[] addrBytes = message.getAddress().getIp().getAddress();
        byte[] addrToWrite = addrBytes.length == 4? new byte[16] : addrBytes;

        if (addrBytes.length == 4) {
            System.arraycopy(addrBytes, 0, addrToWrite, 12, 4);
            addrToWrite[10] = (byte) 0xFF;
            addrToWrite[11] = (byte) 0xFF;
        }
        byteWriter.write(addrToWrite);

        // We write the "Port" field. 2 bytes, network byte order
        int port = message.getAddress().getPort();
        byteWriter.write((byte) (0xFF & port >> 8));
        byteWriter.write((byte) (0xFF & port));
    }
}
