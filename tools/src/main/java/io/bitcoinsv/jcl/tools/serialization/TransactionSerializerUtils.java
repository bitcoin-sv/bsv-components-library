package io.bitcoinsv.jcl.tools.serialization;

import io.bitcoinsv.jcl.tools.bytes.IReader;

public class TransactionSerializerUtils {

    /**
     * It deserializes a single Tx of this block from the Byte Array Reader, and returns it
     */
    public static byte[] deserializeNextTx(IReader byteReader) {
        // We need to locate the position in this Reader that marks te end of the Txs, and then we just "extract" all
        // the bytes from the beginning and up to that point:
        int numBytesInTx = 0;

        // Version
        numBytesInTx += 4;

        // input count
        long inputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(inputCount);

        // txInputs
        for (int i = 0; i < inputCount; i++) {
            // output
            numBytesInTx += 36;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);
            // script
            numBytesInTx += scriptLen;
            // sequence
            numBytesInTx += 4;
        }

        // output count
        long outputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(outputCount);

        // txOutputs
        for (int i = 0; i < outputCount; i++) {
            // Value
            numBytesInTx += 8;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

            //script
            numBytesInTx += scriptLen;
        }
        // lock time
        numBytesInTx += 4;

        return byteReader.read(numBytesInTx);
    }
}
