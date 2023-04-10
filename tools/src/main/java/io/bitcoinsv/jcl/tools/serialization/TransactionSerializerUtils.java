package io.bitcoinsv.jcl.tools.serialization;

import io.bitcoinsv.jcl.tools.bytes.IReader;

public class TransactionSerializerUtils {

    /**
     * It deserializes a single Tx of this block from the Byte Array Reader, and returns it
     */
    public static byte[] deserializeNextTx(IReader byteReader) {
        // We need to locate the position in this Reader that marks te end of the Txs, and then we just "extract" all
        // the bytes from the beginning and up to that point:
        // NOTE: Using long to be able to detect integer overflows that can be caused by malformed transactions.
        //       ArithmeticException will be thrown by Math.toIntExact() if this number is too large for int and
        //       reading will fail (IllegalStateException or ArrayIndexOutOfBoundsException) if there is not enough
        //       data in byteReader.
        long numBytesInTx = 0;

        // Version
        numBytesInTx += 4;

        // input count
        long inputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, Math.toIntExact(numBytesInTx));
        if (inputCount<0) {
            throw new RuntimeException("Invalid number of inputs in transaction!");
        }
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(inputCount);

        // txInputs
        for (int i = 0; i < inputCount; i++) {
            // output
            numBytesInTx += 36;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, Math.toIntExact(numBytesInTx));
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);
            // script
            if (scriptLen<0 || scriptLen>Integer.MAX_VALUE) {
                // NOTE: Sizes larger than Integer.MAX_VALUE will definitely cause ArithmeticException when converted
                //       to int so there is no point in allowing them here, which saves us from also having to check
                //       for possible long overflows.
                throw new RuntimeException("Size of input script in transaction too large!");
            }
            numBytesInTx += scriptLen;
            // sequence
            numBytesInTx += 4;
        }

        // output count
        long outputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, Math.toIntExact(numBytesInTx));
        if (outputCount<0) {
            throw new RuntimeException("Invalid number of outputs in transaction!");
        }
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(outputCount);

        // txOutputs
        for (int i = 0; i < outputCount; i++) {
            // Value
            numBytesInTx += 8;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, Math.toIntExact(numBytesInTx));
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

            //script
            if (scriptLen<0 || scriptLen>Integer.MAX_VALUE) {
                throw new RuntimeException("Size of output script in transaction too large!");
            }
            numBytesInTx += scriptLen;
        }
        // lock time
        numBytesInTx += 4;

        return byteReader.read(Math.toIntExact(numBytesInTx));
    }
}
