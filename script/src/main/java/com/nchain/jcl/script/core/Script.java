/*
 * Copyright 2011 Google Inc.
 * Copyright 2012 Matt Corallo.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2017 Steve Shadders
 *
 * Licensed under the Open BSV licence
 */

package com.nchain.jcl.script.core;

import com.nchain.jcl.base.domain.bean.BitcoinSerializableObjectImpl;
import com.nchain.jcl.base.tools.util.DateTimeUtils;
import com.nchain.jcl.script.exception.ScriptParseException;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static com.nchain.jcl.script.core.ScriptOpCodes.*;
import static com.google.common.base.Preconditions.*;

/**
 * <p>Programs embedded inside transactions that control redemption of payments.</p>
 *
 * <p>Bitcoin transactions don't specify what they do directly. Instead <a href="https://en.bitcoin.it/wiki/Script">a
 * small binary stack language</a> is used to define programs that when evaluated return whether the transaction
 * "accepts" or rejects the other transactions connected to it.</p>
 *
 * <p>In SPV mode, scripts are not run, because that would require all transactions to be available and lightweight
 * clients don't have that data. In full mode, this class is used to run the interpreted language. It also has
 * static methods for building scripts.</p>
 */
@Value
public class Script extends BitcoinSerializableObjectImpl {

    /** Enumeration to encapsulate the type of this script. */
    public enum ScriptType {
        // Do NOT change the ordering of the following definitions because their ordinals are stored in databases.
        NO_TYPE,
        P2PKH,
        PUB_KEY,
        P2SH
    }

    // The program is a set of chunks where each element is either [opcode] or [data, data, data ...]
    private List<ScriptChunk> chunks;

    // Unfortunately, scripts are not ever re-serialized or canonicalized when used in signature hashing. Thus we
    // must preserve the exact bytes that we read off the wire, along with the parsed form.
    private byte[] program;

    // Creation time of the associated keys in seconds since the epoch.
    private long creationTimeSeconds;

    private static final int SIG_SIZE = 75;
    private static final int ADDRESS_LENGTH = 20;

    protected Script() {
        chunks = Collections.unmodifiableList(new ArrayList<ScriptChunk>());
        program = new byte[]{};
        creationTimeSeconds = DateTimeUtils.nowMillisUTC() / 1000;
    }

    protected Script(List<ScriptChunk> chunks) {
        program = new byte[]{};
        this.chunks = Collections.unmodifiableList(new ArrayList<ScriptChunk>(chunks));
        creationTimeSeconds = DateTimeUtils.nowMillisUTC() / 1000;
    }

    protected Script(List<ScriptChunk> chunks, byte[] program) {
        this.chunks = Collections.unmodifiableList(new ArrayList<ScriptChunk>(chunks));
        this.program = program;
        creationTimeSeconds = DateTimeUtils.nowMillisUTC() / 1000;
    }


    /** Convenience methods to get a reference to the Builder */
    public static ScriptBuilder builder() { return new ScriptBuilder();}
    public static ScriptBuilder builder(byte[] program) { return ScriptBuilder.parse(program);}
    public static ScriptBuilder builder(String program) { return ScriptBuilder.parsePlainText(program);}

    /** Returns a Reference to the Builder already pre-filled with thi Script's content */
    public ScriptBuilder toBuilder() {return new ScriptBuilder(this); }

    /**
     * Returns true if this script is of the form <pubkey> OP_CHECKSIG. This form was originally intended for transactions
     * where the peers talked to each other directly via TCP/IP, but has fallen out of favor with time due to that mode
     * of operation being susceptible to man-in-the-middle attacks. It is still used in coinbase outputs and can be
     * useful more exotic types of transaction, but today most payments are to addresses.
     */
    public boolean isSentToRawPubKey() {
        return chunks.size() == 2 && chunks.get(1).equalsOpCode(OP_CHECKSIG) &&
                !chunks.get(0).isOpCode() && chunks.get(0).getData().length() > 1;
    }

    /**
     * Returns true if this script is of the form DUP HASH160 <pubkey hash> EQUALVERIFY CHECKSIG, ie, payment to an
     * address like 1VayNert3x1KzbpzMGt2qdqrAThiRovi8. This form was originally intended for the case where you wish
     * to send somebody money with a written code because their node is offline, but over time has become the standard
     * way to make payments due to the short and recognizable base58 form addresses come in.
     */
    public boolean isSentToAddress() {
        return chunks.size() == 5 &&
                chunks.get(0).equalsOpCode(OP_DUP) &&
                chunks.get(1).equalsOpCode(OP_HASH160) &&
                chunks.get(2).getData().length() == ADDRESS_LENGTH &&
                chunks.get(3).equalsOpCode(OP_EQUALVERIFY) &&
                chunks.get(4).equalsOpCode(OP_CHECKSIG);
    }

    /**
     * <p>If a program matches the standard template DUP HASH160 &lt;pubkey hash&gt; EQUALVERIFY CHECKSIG
     * then this function retrieves the third element.
     * In this case, this is useful for fetching the destination address of a transaction.</p>
     *
     * <p>If a program matches the standard template HASH160 &lt;script hash&gt; EQUAL
     * then this function retrieves the second element.
     * In this case, this is useful for fetching the hash of the redeem script of a transaction.</p>
     *
     * <p>Otherwise it throws a ScriptException.</p>
     */
    public byte[] getPubKeyHash() throws ScriptParseException {
        if (isSentToAddress())
            return chunks.get(2).data();
        else if (isPayToScriptHash())
            return chunks.get(1).data();
        else
            throw new ScriptParseException("Script not in the standard scriptPubKey form");
    }

    /**
     * Returns the public key in this script. If a script contains two constants and nothing else, it is assumed to
     * be a scriptSig (input) for a pay-to-address output and the second constant is returned (the first is the
     * signature). If a script contains a constant and an OP_CHECKSIG opcode, the constant is returned as it is
     * assumed to be a direct pay-to-key scriptPubKey (output) and the first constant is the public key.
     *
     * @throws ScriptParseException if the script is none of the named forms.
     */
    public byte[] getPubKey() throws ScriptParseException {
        if (chunks.size() != 2) {
            throw new ScriptParseException("Script not of right size, expecting 2 but got " + chunks.size());
        }
        final ScriptChunk chunk0 = chunks.get(0);
        final byte[] chunk0data = chunk0.data();
        final ScriptChunk chunk1 = chunks.get(1);
        final byte[] chunk1data = chunk1.data();
        if (chunk0data != null && chunk0data.length > 2 && chunk1data != null && chunk1data.length > 2) {
            // If we have two large constants assume the input to a pay-to-address output.
            return chunk1data;
        } else if (chunk1.equalsOpCode(OP_CHECKSIG) && chunk0data != null && chunk0data.length > 2) {
            // A large constant followed by an OP_CHECKSIG is the key.
            return chunk0data;
        } else {
            throw new ScriptParseException("Script did not match expected form: " + this);
        }
    }

    /**
     * Retrieves the sender public key from a LOCKTIMEVERIFY transaction
     *
     * @throws ScriptParseException
     */
    public byte[] getCLTVPaymentChannelSenderPubKey() throws ScriptParseException {
        if (!isSentToCLTVPaymentChannel()) {
            throw new ScriptParseException("Script not a standard CHECKLOCKTIMVERIFY transaction: " + this);
        }
        return chunks.get(8).data();
    }

    /**
     * Retrieves the recipient public key from a LOCKTIMEVERIFY transaction
     *
     * @throws ScriptParseException
     */
    public byte[] getCLTVPaymentChannelRecipientPubKey() throws ScriptParseException {
        if (!isSentToCLTVPaymentChannel()) {
            throw new ScriptParseException("Script not a standard CHECKLOCKTIMVERIFY transaction: " + this);
        }
        return chunks.get(1).data();
    }

    /**
     * Returns number of signatures required to satisfy this script.
     */
    public int getNumberOfSignaturesRequiredToSpend() {
        if (isSentToMultiSig()) {
            // for N of M CHECKMULTISIG script we will need N signatures to spend
            ScriptChunk nChunk = chunks.get(0);
            return decodeFromOpN(nChunk.getOpcode());
        } else if (isSentToAddress() || isSentToRawPubKey()) {
            // pay-to-address and pay-to-pubkey require single sig
            return 1;
        } else if (isPayToScriptHash()) {
            throw new IllegalStateException("For P2SH number of signatures depends on redeem script");
        } else {
            throw new IllegalStateException("Unsupported script type");
        }
    }

    /**
     * Returns number of bytes required to spend this script. It accepts optional ECKey and redeemScript that may
     * be required for certain types of script to estimate target size.
     */
    public int getNumberOfBytesRequiredToSpend(@Nullable int pubKeyLen, @Nullable Script redeemScript) {
        if (isPayToScriptHash()) {
            // scriptSig: <sig> [sig] [sig...] <redeemscript>
            checkArgument(redeemScript != null, "P2SH script requires redeemScript to be spent");
            return redeemScript.getNumberOfSignaturesRequiredToSpend() * SIG_SIZE + redeemScript.getProgram().length;
        } else if (isSentToMultiSig()) {
            // scriptSig: OP_0 <sig> [sig] [sig...]
            return getNumberOfSignaturesRequiredToSpend() * SIG_SIZE + 1;
        } else if (isSentToRawPubKey()) {
            // scriptSig: <sig>
            return SIG_SIZE;
        } else if (isSentToAddress()) {
            // scriptSig: <sig> <pubkey>
            int uncompressedPubKeySize = 65;
            return SIG_SIZE + (pubKeyLen > 0 ? pubKeyLen : uncompressedPubKeySize);
        } else {
            throw new IllegalStateException("Unsupported script type");
        }
    }



    /**
     * Returns whether this script matches the format used for multisig outputs: [n] [keys...] [m] CHECKMULTISIG
     */
    public boolean isSentToMultiSig() {
        if (chunks.size() < 4) return false;
        ScriptChunk chunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY].
        if (!chunk.isOpCode()) return false;
        if (!(chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY))) return false;
        try {
            // Second to last chunk must be an OP_N opcode and there should be that many data chunks (keys).
            ScriptChunk m = chunks.get(chunks.size() - 2);
            if (!m.isOpCode()) return false;
            int numKeys = decodeFromOpN(m.getOpcode());
            if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) return false;
            }
            // First chunk must be an OP_N opcode too.
            if (decodeFromOpN(chunks.get(0).getOpcode()) < 1) return false;
        } catch (IllegalArgumentException e) { // thrown by decodeFromOpN()
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    public boolean isSentToCLTVPaymentChannel() {
        if (chunks.size() != 10) return false;
        // Check that opcodes match the pre-determined format.
        if (!chunks.get(0).equalsOpCode(OP_IF)) return false;
        // chunk[1] = recipient pubkey
        if (!chunks.get(2).equalsOpCode(OP_CHECKSIGVERIFY)) return false;
        if (!chunks.get(3).equalsOpCode(OP_ELSE)) return false;
        // chunk[4] = locktime
        if (!chunks.get(5).equalsOpCode(OP_CHECKLOCKTIMEVERIFY)) return false;
        if (!chunks.get(6).equalsOpCode(OP_DROP)) return false;
        if (!chunks.get(7).equalsOpCode(OP_ENDIF)) return false;
        // chunk[8] = sender pubkey
        if (!chunks.get(9).equalsOpCode(OP_CHECKSIG)) return false;
        return true;
    }

    public boolean isOpReturnBeforeGenesis() {
        return chunks.size() > 0 && chunks.get(0).equalsOpCode(OP_RETURN);
    }

    public boolean isOpReturnAfterGenesis() {
        return chunks.size() > 1 && chunks.get(0).equalsOpCode(OP_0) && chunks.get(1).equalsOpCode(OP_RETURN);
    }

    /**
     * <p>Whether or not this is a scriptPubKey representing a pay-to-script-hash output. In such outputs, the logic that
     * controls reclamation is not actually in the output at all. Instead there's just a hash, and it's up to the
     * spending input to provide a program matching that hash. This rule is "soft enforced" by the network as it does
     * not exist in Bitcoin Core. It means blocks containing P2SH transactions that don't match
     * correctly are considered valid, but won't be mined upon, so they'll be rapidly re-orgd out of the chain. This
     * logic is defined by <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP 16</a>.</p>
     *
     * <p>bitcoinj does not support creation of P2SH transactions today. The goal of P2SH is to allow short addresses
     * even for complex scripts (eg, multi-sig outputs) so they are convenient to work with in things like QRcodes or
     * with copy/paste, and also to minimize the size of the unspent output set (which improves performance of the
     * Bitcoin system).</p>
     */
    public boolean isPayToScriptHash() {
        // We have to check against the serialized form because BIP16 defines a P2SH output using an exact byte
        // template, not the logical program structure. Thus you can have two programs that look identical when
        // printed out but one is a P2SH script and the other isn't! :(
        byte[] program = getProgram();
        return program.length == 23 &&
                (program[0] & 0xff) == OP_HASH160 &&
                (program[1] & 0xff) == 0x14 &&
                (program[22] & 0xff) == OP_EQUAL;
    }

    /**
     * Get the {@link ScriptType}.
     *
     * @return The script type.
     */
    public ScriptType getScriptType() {
        ScriptType type = ScriptType.NO_TYPE;
        if (isSentToAddress()) {
            type = ScriptType.P2PKH;
        } else if (isSentToRawPubKey()) {
            type = ScriptType.PUB_KEY;
        } else if (isPayToScriptHash()) {
            type = ScriptType.P2SH;
        }
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Arrays.equals(program, ((Script) o).program);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(program);
    }

}
