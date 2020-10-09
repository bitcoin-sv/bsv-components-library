package com.nchain.jcl.script.interpreter;


import com.google.common.collect.Lists;
import com.nchain.jcl.base.core.Address;
import com.nchain.jcl.base.core.Addressable;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.base.tools.bytes.UnsafeByteArrayOutputStream;
import com.nchain.jcl.base.tools.crypto.*;

import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.core.Script;
import com.nchain.jcl.script.core.ScriptChunk;
import com.nchain.jcl.script.exception.ScriptExecutionException;
import com.nchain.jcl.script.exception.ScriptParseException;
import com.nchain.jcl.script.serialization.ScriptChunkSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.nchain.jcl.script.core.ScriptOpCodes.*;

public class ScriptUtils {

    static final Logger log = LoggerFactory.getLogger(ScriptUtils.class);

    private static final int[] RSHIFT_MASKS = new int[]{0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80};
    private static final int[] LSHIFT_MASKS = new int[]{0xFF, 0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01};

    private static int getSigOpCount(List<ScriptChunk> chunks, boolean accurate) throws ScriptParseException {
        int sigOps = 0;
        int lastOpCode = OP_INVALIDOPCODE;
        for (ScriptChunk chunk : chunks) {
            if (chunk.isOpCode()) {
                switch (chunk.getOpcode()) {
                    case OP_CHECKSIG:
                    case OP_CHECKSIGVERIFY:
                        sigOps++;
                        break;
                    case OP_CHECKMULTISIG:
                    case OP_CHECKMULTISIGVERIFY:
                        if (accurate && lastOpCode >= OP_1 && lastOpCode <= OP_16)
                            sigOps += decodeFromOpN(lastOpCode);
                        else
                            sigOps += 20;
                        break;
                    default:
                        break;
                }
                lastOpCode = chunk.getOpcode();
            }
        }
        return sigOps;
    }

    /**
     * Gets the count of regular SigOps in the script program (counting multisig ops as 20)
     */
    public static int getSigOpCount(byte[] program) throws ScriptParseException {
        Script script = null;
        try {
            script = Script.builder(program).build();
        } catch (ScriptParseException e) {
            // Ignore errors and count up to the parse-able length
        }
        return getSigOpCount(script.getChunks(), false);
    }

    /**
     * Gets the count of P2SH Sig Ops in the Script scriptSig
     */
    public static long getP2SHSigOpCount(byte[] scriptSig) throws ScriptExecutionException {
        Script script = null;
        try {
            script = Script.builder(scriptSig).build();
        } catch (ScriptExecutionException e) {
            // Ignore errors and count up to the parse-able length
        }
        for (int i = script.getChunks().size() - 1; i >= 0; i--)
            if (!script.getChunks().get(i).isOpCode()) {
                Script subScript = Script.builder(script.getChunks().get(i).data()).build();
                return getSigOpCount(subScript.getChunks(), true);
            }
        return 0;
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     */
    public static Addressable getToAddress(Script script, ScriptConfig scriptConfig) throws ScriptExecutionException {
        return getToAddress(script, scriptConfig, false);
    }

    /**
     * Gets the destination address from this script, if it's in the required form (see getPubKey).
     *
     * @param script
     * @param forcePayToPubKey If true, allow payToPubKey to be casted to the corresponding address. This is useful if you prefer
     *                         showing addresses rather than pubkeys.
     */
    public static Addressable getToAddress(Script script, ScriptConfig scriptConfig, boolean forcePayToPubKey) throws ScriptExecutionException {
        if (script.isSentToAddress())
            return new Address(scriptConfig.getAddressHeader(), script.getPubKeyHash());
        else if (script.isPayToScriptHash())
            return fromP2SHScript(scriptConfig, script);
        else if (forcePayToPubKey && script.isSentToRawPubKey())
            return new Address(scriptConfig.getAddressHeader(), Sha256.sha256hash160(script.getPubKey()));
        else
            throw new ScriptExecutionException("Cannot cast this script to a pay-to-address type");
    }

    /** Returns an Address that represents the script hash extracted from the given scriptPubKey */
    public static Addressable fromP2SHScript(ScriptConfig scriptConfig, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return new Address(scriptConfig.getP2shHeader(), scriptPubKey.getPubKeyHash());
    }

    /**
     * Creates a program that requires at least N of the given keys to sign, using OP_CHECKMULTISIG.
     */
    public static byte[] createMultiSigOutputScript(int threshold, List<? extends ECKeyBytes> pubkeys) {
        checkArgument(threshold > 0);
        checkArgument(threshold <= pubkeys.size());
        checkArgument(pubkeys.size() <= 16);  // That's the max we can represent with a single opcode.
        if (pubkeys.size() > 3) {
            ScriptUtils.log.warn("Creating a multi-signature output that is non-standard: {} pubkeys, should be <= 3", pubkeys.size());
        }

            ByteArrayOutputStream bits = new ByteArrayOutputStream();
            bits.write(encodeToOpN(threshold));
            for (ECKeyBytes key : pubkeys) {
                ScriptChunkSerializer.getInstance().serialize(null, key.getPubKey(), bits);
            }
            bits.write(encodeToOpN(pubkeys.size()));
            bits.write(OP_CHECKMULTISIG);
            return bits.toByteArray();

    }

    public static byte[] createInputScript(byte[] signature, byte[] pubkey) {

            // TODO: Do this by creating a Script *first* then having the script reassemble itself into bytes.
            ByteArrayOutputStream bits = new UnsafeByteArrayOutputStream(signature.length + pubkey.length + 2);
            ScriptChunkSerializer.getInstance().serialize(null, signature, bits);
            ScriptChunkSerializer.getInstance().serialize(null, pubkey, bits);
            return bits.toByteArray();

    }

    public static byte[] createInputScript(byte[] signature) {

            // TODO: Do this by creating a Script *first* then having the script reassemble itself into bytes.
            ByteArrayOutputStream bits = new UnsafeByteArrayOutputStream(signature.length + 2);
            ScriptChunkSerializer.getInstance().serialize(null, signature, bits);
            return bits.toByteArray();

    }

    /**
     * Returns a list of the keys required by this script, assuming a multi-sig script.
     *
     * @throws ScriptExecutionException if the script type is not understood or is pay to address or is P2SH (run this method on the "Redeem script" instead).
     * @param script
     */
    public static List<ECKeyBytes> getPubKeys(Script script) {
        if (!script.isSentToMultiSig())
            throw new ScriptExecutionException("Only usable for multisig scripts.");

        ArrayList<ECKeyBytes> result = Lists.newArrayList();
        int numKeys = decodeFromOpN(script.getChunks().get(script.getChunks().size() - 2).getOpcode());
        for (int i = 0; i < numKeys; i++)
            result.add(new BasicECKeyBytes(script.getChunks().get(1 + i).data()));
        return result;
    }

    /**
     * Returns the index where a signature by the key should be inserted.  Only applicable to
     * a P2SH scriptSig.
     */
    public static int getSigInsertionIndex(Script script, Sha256Wrapper hash, byte[] signingKey) {
        // Iterate over existing signatures, skipping the initial OP_0, the final redeem script
        // and any placeholder OP_0 sigs.
        List<ScriptChunk> existingChunks = script.getChunks().subList(1, script.getChunks().size() - 1);
        ScriptChunk redeemScriptChunk = script.getChunks().get(script.getChunks().size() - 1);
        checkNotNull(redeemScriptChunk.data());
        Script redeemScript = Script.builder(redeemScriptChunk.data()).build();

        int sigCount = 0;
        int myIndex = findKeyInRedeem(redeemScript, signingKey);
        for (ScriptChunk chunk : existingChunks) {
            if (chunk.getOpcode() == OP_0) {
                // OP_0, skip
            } else {
                checkNotNull(chunk.data());
                if (myIndex < findSigInRedeem(redeemScript.getChunks(), chunk.data(), hash))
                    return sigCount;
                sigCount++;
            }
        }
        return sigCount;
    }

    private static int findKeyInRedeem(Script script, byte[] key) {
        checkArgument(script.getChunks().get(0).isOpCode()); // P2SH scriptSig
        int numKeys = decodeFromOpN(script.getChunks().get(script.getChunks().size() - 2).getOpcode());
        for (int i = 0; i < numKeys; i++) {
            if (Arrays.equals(script.getChunks().get(1 + i).data(), key)) {
                return i;
            }
        }

        throw new IllegalStateException("Could not find matching key " + Arrays.toString(key) + " in script " + script);
    }

    private static int findSigInRedeem(List<ScriptChunk> chunks, byte[] signatureBytes, Sha256Wrapper hash) {
        checkArgument(chunks.get(0).isOpCode()); // P2SH scriptSig
        int numKeys = decodeFromOpN(chunks.get(chunks.size() - 2).getOpcode());
        for (int i = 0; i < numKeys; i++) {
            byte[] maybePubkey = chunks.get(i + 1).data();
            //this shouldn't really be here but we are trying to remove dependencies on ECKey.  Looks like this may
            //be decoding then reencoding but it's for a wallet class we plan to delete so it can stay for now.
            if (ECDSA.verify(hash.getBytes(), signatureBytes, ECDSA.CURVE.getCurve().decodePoint(maybePubkey).getEncoded()))
                return i;
        }

        throw new IllegalStateException("Could not find matching key for signature on " + hash.toString() + " sig " + HEX.encode(signatureBytes));
    }


    public static boolean castToBool(StackItem data) {
        return castToBool(data.bytes());
    }

    public static boolean castToBool(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            // "Can be negative zero" - Bitcoin Core (see OpenSSL's BN_bn2mpi)
            if (data[i] != 0)
                return !(i == data.length - 1 && (data[i] & 0xFF) == 0x80);
        }
        return false;
    }


    /**
     * Cast a script chunk to a BigInteger.
     * <p>
     * Post Genesis this is the default
     *
     * @param maxLength the maximum length in bytes.
     * @throws ScriptExecutionException if the chunk is longer than the specified maximum.
     */
    static BigInteger castToBigInteger(ScriptInterpreter.ScriptExecutionState state, final StackItem chunk, final int maxLength, boolean enforceMinimal) throws ScriptExecutionException {
        if (chunk.length() > maxLength)
            throw new ScriptExecutionException(state, "Script attempted to use an integer larger than "
                    + maxLength + " bytes");
        if (enforceMinimal && !ByteTools.checkMinimallyEncodedLE(chunk.bytes(), maxLength))
            throw new ScriptExecutionException(state, "Number is not minimally encoded");
        return ByteTools.decodeMPI(ByteTools.reverseBytes(chunk.bytes()), false);
    }

    /**
     * shift x right by n bits, implements OP_RSHIFT
     * see: https://github.com/bitcoin-sv/bitcoin-sv/commit/27d24de643dbd3cc852e1de7c90e752e19abb9d8
     * <p>
     * Note this does not support shifting more than Integer.MAX_VALUE
     *
     * @param xItem
     * @param n
     * @return
     */
    public static byte[] rShift(StackItem xItem, int n) {
        byte[] x = xItem.bytes();

        int bit_shift = n % 8;
        int byte_shift = n / 8;

        int mask = RSHIFT_MASKS[bit_shift];
        int overflow_mask = (~mask) & 0xff;

        byte[] result = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            int k = i + byte_shift;
            if (k < x.length) {
                int val = x[i] & mask;
                val = val >>> bit_shift;
                result[k] |= val;
            }

            if (k + 1 < x.length) {
                int carryval = x[i] & overflow_mask;
                carryval <<= 8 - bit_shift;
                result[k + 1] |= carryval;
            }
        }
        return result;
    }

    /**
     * shift x left by n bits, implements OP_LSHIFT
     * see: https://github.com/bitcoin-sv/bitcoin-sv/commit/27d24de643dbd3cc852e1de7c90e752e19abb9d8
     * <p>
     * Note this does not support shifting more than Integer.MAX_VALUE
     *
     * @param xItem
     * @param n
     * @return
     */
    public static byte[] lShift(StackItem xItem, int n) {
        byte[] x = xItem.bytes();
        int bit_shift = n % 8;
        int byte_shift = n / 8;

        int mask = LSHIFT_MASKS[bit_shift];
        int overflow_mask = (~mask) & 0xff;

        byte[] result = new byte[x.length];
        for (int i = x.length - 1; i >= 0; i--) {
            int k = i - byte_shift;
            if (k >= 0) {
                int val = x[i] & mask;
                val <<= bit_shift;
                result[k] |= val;
            }

            if (k - 1 >= 0) {
                int carryval = x[i] & overflow_mask;
                carryval = carryval >>> 8 - bit_shift;
                result[k - 1] |= carryval;
            }
        }
        return result;
    }
}
