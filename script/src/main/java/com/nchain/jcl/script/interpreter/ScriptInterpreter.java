package com.nchain.jcl.script.interpreter;


import com.nchain.jcl.base.core.Coin;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.base.tools.bytes.UnsafeByteArrayOutputStream;
import com.nchain.jcl.base.tools.crypto.ECDSA;
import com.nchain.jcl.base.tools.crypto.Sha256;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import com.nchain.jcl.script.JCLScriptConstants;
import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.config.provided.ScriptBSVGenesisConfig;
import com.nchain.jcl.script.core.*;
import com.nchain.jcl.script.exception.ScriptExecutionException;
import com.nchain.jcl.script.serialization.ScriptChunkSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.RIPEMD160Digest;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.nchain.jcl.script.core.ScriptOpCodes.*;


/**
 * @author Steve Shadders.
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is the actual script interpreter than runs Bitcoin Scripts.  It is completely stateless and contians
 * only static methods.  The script itself is now encapsulated in the Script class which uses only instance methods.
 *
 * Some other utility methods are in ScriptUtils, this is main there to assist in untangling the web of dependencies
 * so we can break BitcoinJ up into logically grouped libs.
 */
public class ScriptInterpreter {

    static final Logger log = LoggerFactory.getLogger(ScriptInterpreter.class);

    private final String id;
    // If the scriptConfig is set, then it will be used by default when calling "execute"
    private ScriptConfig scriptConfig;

    /**
     * Definition of the State of the Script execution at a specific point in time
     */
    public static class ScriptExecutionState {
        public ScriptConfig config;
        public ScriptStack stack;
        public List<StackItem> stackPopped;
        public ScriptStack altStack;
        public List<StackItem> altStackPopped;
        public LinkedList<Boolean> ifStack;
        public ScriptStream script;
        public int opCount;
        public ScriptChunk lastOpCode;
        public ScriptChunk currentOpCode;
        public int currentOpCodeIndex = 0;
        public Set<ScriptVerifyFlag> verifyFlags;
        public boolean initialStackStateKnown;
    }


    /** Protected constructor. Use InterpreterBuilder instead */
    protected ScriptInterpreter(String id, ScriptConfig scriptConfig) {
        this.id = id;
        this.scriptConfig = scriptConfig;
    }

    // Convenience method to get a reference to the Builder
    public static ScriptInterpreterBuilder builder(String id) { return new ScriptInterpreterBuilder(id);}


    /**
     * Executes the script interpreter. It executes a single Script. This method is low-level, since it only executes
     * one single script. for the more common scenario when you want to check if a Output is Spendable, you should
     * use correctlySpends instead.
     */
    public ScriptResult execute(ScriptConfig scriptConfig,
                                ScriptContext scriptContext, Script script) throws ScriptExecutionException {

        try {
            return execute(scriptConfig, scriptContext, new SimpleScriptStream(script));
        } catch (ScriptExecutionException e) {
            if (scriptContext.getListener() != null) {
                scriptContext.getListener().onExceptionThrown(e);
                //pause to hopefully give the System.out time to beat System.err
                try { Thread.sleep(200); } catch (InterruptedException e1) { e1.printStackTrace();}
            }
            throw e;
        }
    }

    /**
     * convenience method, where no ScriptConfig is provided, In this case, the Configuration is taken from the one
     * assigned to the Interpreter at the moment of its creation, or the "Genesis" Default One
     */
    public ScriptResult execute(ScriptContext scriptContext, Script script) throws ScriptExecutionException {

        ScriptConfig scriptConfig = (this.scriptConfig != null) ? this.scriptConfig : new ScriptBSVGenesisConfig();
        return execute(scriptConfig, scriptContext, script);
    }

    /**
     * Executes the script interpreter. It executes a single Script. This method is low-level, since it only executes
     * one single script. for the more common scenario when you want to check if a Output is Spendable, you should
     * use correctlySpends instead.
     */
    public ScriptResult execute(ScriptConfig scriptConfig,
                                ScriptContext scriptContext,
                                ScriptStream script) throws ScriptExecutionException {
        int opCount = 0;
        int lastCodeSepLocation = 0;

        //for scriptSig this can be set to true as the stack state is known to be empty
        boolean initialStackStateKnown = false;

        //mark all stack items as derived if initial stack state is not known to this execution context
        ScriptStack stack = scriptContext.getStack();
        ScriptStateListener scriptStateListener = scriptContext.getListener();
        Tx txContainingThis = scriptContext.getTxContainingScript();
        long index = scriptContext.getTxInputIndex();
        Coin value = scriptContext.getValue();

        stack.setDerivations(!initialStackStateKnown);
        ScriptStack altstack = new ScriptStack();
        LinkedList<Boolean> ifStack = new LinkedList<Boolean>();
        final boolean enforceMinimal = scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.MINIMALDATA);
        final boolean genesisActive = scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.GENESIS_OPCODES);
        final long maxScriptElementSize = scriptConfig.getMaxDataSizeInBytes();
        final int maxNumElementSize = scriptConfig.getMaxNumberSizeInBytes();
        final int maxMultisigKeys = scriptConfig.getMaxNumMultisigPubkeys();
        final int maxOpCount = scriptConfig.getMaxOpCount();

        if (scriptStateListener != null) {
            scriptStateListener.setInitialState(
                    scriptContext.getTxContainingScript(),
                    scriptContext.getTxInputIndex(),
                    script,
                    Collections.unmodifiableList(stack),
                    Collections.unmodifiableList(altstack),
                    Collections.unmodifiableList(ifStack),
                    value,
                    scriptConfig.getVerifyFlags()
            );
        }

        boolean opReturnCalled = false;

        //initialise script state tracker
        ScriptExecutionState state = new ScriptExecutionState();
        state.config = scriptConfig;
        state.stack = stack;
        state.stackPopped = stack.getPoppedItems();
        state.altStack = altstack;
        state.altStackPopped = altstack.getPoppedItems();
        state.ifStack = ifStack;
        state.opCount = 0;
        state.verifyFlags = scriptConfig.getVerifyFlags();
        state.script = script;
        state.initialStackStateKnown = initialStackStateKnown;

        for (ScriptChunk chunk : script) {
            state.lastOpCode = state.currentOpCode;
            state.currentOpCode = chunk;
            state.currentOpCodeIndex++;

            //clear tracked popped items from stack
            stack.clearPoppedItems();
            altstack.clearPoppedItems();

            boolean shouldExecute = !ifStack.contains(false);

            if (scriptStateListener != null) {
                scriptStateListener._onBeforeOpCodeExecuted(chunk, shouldExecute);
            }

            if (chunk.getOpcode() == OP_0) {
                if (!shouldExecute)
                    continue;

                stack.add(new byte[]{});
            } else if (!chunk.isOpCode()) {
                if (chunk.getData().length() > maxScriptElementSize)
                    throw new ScriptExecutionException(state, "Attempted to push a data string larger than " + maxScriptElementSize + " bytes");

                if (!shouldExecute)
                    continue;

                stack.add(chunk.data());
            } else {
                int opcode = chunk.getOpcode();
                if (opcode > OP_16) {
                    opCount++;
                    state.opCount = opCount;
                    if (opCount > maxOpCount)
                        throw new ScriptExecutionException(state, "More script operations than is allowed");
                }

                if (opcode == OP_VERIF || opcode == OP_VERNOTIF)
                    throw new ScriptExecutionException(state, "Script included OP_VERIF or OP_VERNOTIF");

                // Some opcodes are disabled.
                if (scriptConfig.isOpCodeDisabled(opcode)) {
                    throw new ScriptExecutionException(state, "Script included a disabled Script Op.");
                }

                switch (opcode) {
                    case OP_IF:
                        if (!shouldExecute) {
                            ifStack.add(false);
                            continue;
                        }
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_IF on an empty stack");
                        ifStack.add(ScriptUtils.castToBool(stack.pollLast().bytes()));
                        continue;
                    case OP_NOTIF:
                        if (!shouldExecute) {
                            ifStack.add(false);
                            continue;
                        }
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_NOTIF on an empty stack");
                        ifStack.add(!ScriptUtils.castToBool(stack.pollLast().bytes()));
                        continue;
                    case OP_ELSE:
                        if (ifStack.isEmpty())
                            throw new ScriptExecutionException(state, "Attempted OP_ELSE without OP_IF/NOTIF");
                        ifStack.add(!ifStack.pollLast());
                        continue;
                    case OP_ENDIF:
                        if (ifStack.isEmpty())
                            throw new ScriptExecutionException(state, "Attempted OP_ENDIF without OP_IF/NOTIF");
                        ifStack.pollLast();
                        continue;
                }

                if (!shouldExecute)
                    continue;

                switch (opcode) {
                    // OP_0 is no opcode
                    case OP_1NEGATE:
                        stack.add(ByteTools.reverseBytes(ByteTools.encodeMPI(BigInteger.ONE.negate(), false)));
                        break;
                    case OP_1:
                    case OP_2:
                    case OP_3:
                    case OP_4:
                    case OP_5:
                    case OP_6:
                    case OP_7:
                    case OP_8:
                    case OP_9:
                    case OP_10:
                    case OP_11:
                    case OP_12:
                    case OP_13:
                    case OP_14:
                    case OP_15:
                    case OP_16:
                        stack.add(StackItem.forSmallNum(decodeFromOpN(opcode)));
                        break;
                    case OP_NOP:
                        break;
                    case OP_VERIFY:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_VERIFY on an empty stack");
                        if (!ScriptUtils.castToBool(stack.pollLast().bytes()))
                            throw new ScriptExecutionException(state, "OP_VERIFY failed");
                        break;
                    case OP_RETURN:
                        if (!scriptConfig.isExceptionThrownOnOpReturn()) {
                            //will exit at end of loop so all checks are completed.
                            opReturnCalled = true;
                        } else {
                            throw new ScriptExecutionException(state, "Script called OP_RETURN");
                        }
                    case OP_TOALTSTACK:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_TOALTSTACK on an empty stack");
                        altstack.add(stack.pollLast());
                        break;
                    case OP_FROMALTSTACK:
                        if (altstack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_FROMALTSTACK on an empty altstack");
                        stack.add(altstack.pollLast());
                        break;
                    case OP_2DROP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_2DROP on a stack with size < 2");
                        stack.pollLast();
                        stack.pollLast();
                        break;
                    case OP_2DUP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_2DUP on a stack with size < 2");
                        Iterator<StackItem> it2DUP = stack.descendingIterator();
                        StackItem OP2DUPtmpChunk2 = it2DUP.next();
                        stack.add(it2DUP.next());
                        stack.add(OP2DUPtmpChunk2);
                        break;
                    case OP_3DUP:
                        if (stack.size() < 3)
                            throw new ScriptExecutionException(state, "Attempted OP_3DUP on a stack with size < 3");
                        Iterator<StackItem> it3DUP = stack.descendingIterator();
                        StackItem OP3DUPtmpChunk3 = it3DUP.next();
                        StackItem OP3DUPtmpChunk2 = it3DUP.next();
                        stack.add(it3DUP.next());
                        stack.add(OP3DUPtmpChunk2);
                        stack.add(OP3DUPtmpChunk3);
                        break;
                    case OP_2OVER:
                        if (stack.size() < 4)
                            throw new ScriptExecutionException(state, "Attempted OP_2OVER on a stack with size < 4");
                        Iterator<StackItem> it2OVER = stack.descendingIterator();
                        it2OVER.next();
                        it2OVER.next();
                        StackItem OP2OVERtmpChunk2 = it2OVER.next();
                        stack.add(it2OVER.next());
                        stack.add(OP2OVERtmpChunk2);
                        break;
                    case OP_2ROT:
                        if (stack.size() < 6)
                            throw new ScriptExecutionException(state, "Attempted OP_2ROT on a stack with size < 6");
                        StackItem OP2ROTtmpChunk6 = stack.pollLast();
                        StackItem OP2ROTtmpChunk5 = stack.pollLast();
                        StackItem OP2ROTtmpChunk4 = stack.pollLast();
                        StackItem OP2ROTtmpChunk3 = stack.pollLast();
                        StackItem OP2ROTtmpChunk2 = stack.pollLast();
                        StackItem OP2ROTtmpChunk1 = stack.pollLast();
                        stack.add(OP2ROTtmpChunk3);
                        stack.add(OP2ROTtmpChunk4);
                        stack.add(OP2ROTtmpChunk5);
                        stack.add(OP2ROTtmpChunk6);
                        stack.add(OP2ROTtmpChunk1);
                        stack.add(OP2ROTtmpChunk2);
                        break;
                    case OP_2SWAP:
                        if (stack.size() < 4)
                            throw new ScriptExecutionException(state, "Attempted OP_2SWAP on a stack with size < 4");
                        StackItem OP2SWAPtmpChunk4 = stack.pollLast();
                        StackItem OP2SWAPtmpChunk3 = stack.pollLast();
                        StackItem OP2SWAPtmpChunk2 = stack.pollLast();
                        StackItem OP2SWAPtmpChunk1 = stack.pollLast();
                        stack.add(OP2SWAPtmpChunk3);
                        stack.add(OP2SWAPtmpChunk4);
                        stack.add(OP2SWAPtmpChunk1);
                        stack.add(OP2SWAPtmpChunk2);
                        break;
                    case OP_IFDUP:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_IFDUP on an empty stack");
                        StackItem ifdupBool = stack.getLast();
                        if (ScriptUtils.castToBool(ifdupBool.bytes()))
                            stack.add(stack.getLast(), ifdupBool);
                        break;
                    case OP_DEPTH:
                        //depth can't be known at runtime unless you already know the size of the initial stack.
                        stack.add(StackItem.wrapDerived(ByteTools.reverseBytes(ByteTools.encodeMPI(BigInteger.valueOf(stack.size()), false)), true));
                        break;
                    case OP_DROP:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_DROP on an empty stack");
                        stack.pollLast();
                        break;
                    case OP_DUP:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_DUP on an empty stack");
                        stack.add(stack.getLast());
                        break;
                    case OP_NIP:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_NIP on a stack with size < 2");
                        StackItem OPNIPtmpChunk = stack.pollLast();
                        stack.pollLast();
                        stack.add(OPNIPtmpChunk);
                        break;
                    case OP_OVER:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_OVER on a stack with size < 2");
                        Iterator<StackItem> itOVER = stack.descendingIterator();
                        itOVER.next();
                        stack.add(itOVER.next());
                        break;
                    case OP_PICK:
                    case OP_ROLL:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_PICK/OP_ROLL on an empty stack");

                        StackItem rollVal = stack.pollLast();

                        long val = ScriptUtils.castToBigInteger(state, rollVal, maxNumElementSize, enforceMinimal).longValue();
                        if (val < 0 || val >= stack.size())
                            throw new ScriptExecutionException(state, "OP_PICK/OP_ROLL attempted to get data deeper than stack size");
                        Iterator<StackItem> itPICK = stack.descendingIterator();
                        for (long i = 0; i < val; i++)
                            itPICK.next();
                        StackItem OPROLLtmpChunk = itPICK.next();
                        if (opcode == OP_ROLL)
                            itPICK.remove();
                        //whether the value is derived doesn't depend on where in the stack
                        //it's picked from so just add the original StackItem
                        stack.add(OPROLLtmpChunk);
                        break;
                    case OP_ROT:
                        if (stack.size() < 3)
                            throw new ScriptExecutionException(state, "Attempted OP_ROT on a stack with size < 3");
                        StackItem OPROTtmpChunk3 = stack.pollLast();
                        StackItem OPROTtmpChunk2 = stack.pollLast();
                        StackItem OPROTtmpChunk1 = stack.pollLast();
                        stack.add(OPROTtmpChunk2);
                        stack.add(OPROTtmpChunk3);
                        stack.add(OPROTtmpChunk1);
                        break;
                    case OP_SWAP:
                    case OP_TUCK:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_SWAP on a stack with size < 2");
                        StackItem OPSWAPtmpChunk2 = stack.pollLast();
                        StackItem OPSWAPtmpChunk1 = stack.pollLast();
                        stack.add(OPSWAPtmpChunk2);
                        stack.add(OPSWAPtmpChunk1);
                        if (opcode == OP_TUCK)
                            stack.add(OPSWAPtmpChunk2);
                        break;
                    //byte string operations
                    case OP_CAT:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem catBytes2 = stack.pollLast();
                        StackItem catBytes1 = stack.pollLast();

                        int len = catBytes1.length() + catBytes2.length();
                        if (len > maxScriptElementSize)
                            throw new ScriptExecutionException(state, "Push value size limit exceeded.");

                        byte[] catOut = new byte[len];
                        System.arraycopy(catBytes1.bytes(), 0, catOut, 0, catBytes1.length());
                        System.arraycopy(catBytes2.bytes(), 0, catOut, catBytes1.length(), catBytes2.length());
                        stack.add(catOut, catBytes1, catBytes2);

                        break;

                    case OP_SPLIT:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem biSplitPosItem = stack.pollLast();
                        StackItem splitBytesItem = stack.pollLast();
                        BigInteger biSplitPos = ScriptUtils.castToBigInteger(state, biSplitPosItem, maxNumElementSize, enforceMinimal);

                        //sanity check in case we aren't enforcing minimal number encoding
                        //we will check that the biSplitPos value can be safely held in an int
                        //before we cast it as BigInteger will behave similar to casting if the value
                        //is greater than the target type can hold.
                        BigInteger biMaxInt = BigInteger.valueOf((long) Integer.MAX_VALUE);
                        if (biSplitPos.compareTo(biMaxInt) >= 0)
                            throw new ScriptExecutionException(state, "Invalid OP_SPLIT range.");

                        int splitPos = biSplitPos.intValue();
                        byte[] splitBytes = splitBytesItem.bytes();

                        if (splitPos > splitBytes.length || splitPos < 0)
                            throw new ScriptExecutionException(state, "Invalid OP_SPLIT range.");

                        byte[] splitOut1 = new byte[splitPos];
                        byte[] splitOut2 = new byte[splitBytes.length - splitPos];

                        System.arraycopy(splitBytes, 0, splitOut1, 0, splitPos);
                        System.arraycopy(splitBytes, splitPos, splitOut2, 0, splitOut2.length);

                        stack.add(splitOut1, splitBytesItem, biSplitPosItem);
                        stack.add(splitOut2, splitBytesItem, biSplitPosItem);
                        break;

                    case OP_NUM2BIN:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");

                        StackItem numSizeItem = stack.pollLast();
                        int numSize = ScriptUtils.castToBigInteger(state, numSizeItem, maxNumElementSize, enforceMinimal).intValue();

                        if (numSize > maxScriptElementSize)
                            throw new ScriptExecutionException(state, "Push value size limit exceeded.");

                        StackItem rawNumItem = stack.pollLast();

                        // Try to see if we can fit that number in the number of
                        // byte requested.
                        byte[] minimalNumBytes = ByteTools.minimallyEncodeLE(rawNumItem.bytes());
                        if (minimalNumBytes.length > numSize) {
                            //we can't
                            throw new ScriptExecutionException(state, "The requested encoding is impossible to satisfy.");
                        }

                        if (minimalNumBytes.length == numSize) {
                            //already the right size so just push it to stack
                            stack.add(minimalNumBytes, numSizeItem, rawNumItem);
                        } else if (numSize == 0) {
                            stack.add(ByteTools.EMPTY_BYTE_ARRAY, numSizeItem, rawNumItem);
                        } else {
                            int signBit = 0x00;
                            if (minimalNumBytes.length > 0) {
                                signBit = minimalNumBytes[minimalNumBytes.length - 1] & 0x80;
                                minimalNumBytes[minimalNumBytes.length - 1] &= 0x7f;
                            }
                            int minimalBytesToCopy = minimalNumBytes.length > numSize ? numSize : minimalNumBytes.length;
                            byte[] expandedNumBytes = new byte[numSize]; //initialized to all zeroes
                            System.arraycopy(minimalNumBytes, 0, expandedNumBytes, 0, minimalBytesToCopy);
                            expandedNumBytes[expandedNumBytes.length - 1] = (byte) signBit;
                            stack.add(expandedNumBytes, rawNumItem, numSizeItem);
                        }
                        break;

                    case OP_BIN2NUM:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem binBytes = stack.pollLast();
                        byte[] numBytes = ByteTools.minimallyEncodeLE(binBytes.bytes());

                        if (!ByteTools.checkMinimallyEncodedLE(numBytes, maxNumElementSize))
                            throw new ScriptExecutionException(state, "Given operand is not a number within the valid range [-2^31...2^31]");

                        stack.add(numBytes, binBytes);

                        break;
                    case OP_SIZE:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SIZE on an empty stack");
                        StackItem sizeItem = stack.getLast();
                        stack.add(ByteTools.reverseBytes(ByteTools.encodeMPI(BigInteger.valueOf(sizeItem.length()), false)), sizeItem);
                        break;
                    case OP_INVERT:
                        // (x -- out)
                        if (stack.size() < 1) {
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        }
                        StackItem invertItem = stack.pollLast();
                        byte[] invertBytes = invertItem.wrappedBytes().data();
                        for (int i = 0; i < invertItem.length(); i++) {
                            invertBytes[i] = (byte) ~invertItem.bytes()[i];
                        }
                        ScriptData newDataInverted = ScriptData.builder().data(invertBytes).build();
                        stack.add(StackItem.forBytes(newDataInverted, invertItem.getType(), invertItem));
                        break;

                    case OP_AND:
                    case OP_OR:
                    case OP_XOR:
                        // (x1 x2 - out)
                        if (stack.size() < 2) {
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        }

                        //valtype &vch1 = stacktop(-2);
                        //valtype &vch2 = stacktop(-1);
                        StackItem vch2Item = stack.pollLast();
                        StackItem vch1Item = stack.pollLast();

                        byte[] vch2 = vch2Item.bytes();
                        byte[] vch1 = vch1Item.bytes();

                        // The StackItem class is IMMUTABLE, that means that when get get a reference to its internal
                        // bytes, that's a COY of the original ones. We'll make the hcanges on this copy, and then
                        // we'll build a new Item out of them...

                        // Inputs must be the same size
                        if (vch1.length != vch2.length) {
                            throw new ScriptExecutionException(state, "Invalid operand size.");
                        }

                        // To avoid allocating, we modify vch1 in place.
                        switch (opcode) {
                            case OP_AND:
                                for (int i = 0; i < vch1.length; i++) {
                                    vch1[i] &= vch2[i];
                                }
                                break;
                            case OP_OR:
                                for (int i = 0; i < vch1.length; i++) {
                                    vch1[i] |= vch2[i];
                                }
                                break;
                            case OP_XOR:
                                for (int i = 0; i < vch1.length; i++) {
                                    vch1[i] ^= vch2[i];
                                }
                                break;
                            default:
                                break;
                        }

                        // We use the modified bytes to create a new StackData, and a new StackItem out of it
                        ScriptData newDataLogic = ScriptData.builder().data(vch1).build();
                        StackItem newItem = StackItem.forBytes(newDataLogic, vch1Item.getType(), vch1Item, vch2Item);
                        stack.add(newItem);

                        break;
                    case OP_LSHIFT:
                    case OP_RSHIFT:
                        // (x n -- out)
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Invalid stack operation.");
                        StackItem shiftNItem = stack.pollLast();
                        StackItem shiftData = stack.pollLast();
                        int shiftN = ScriptUtils.castToBigInteger(state, shiftNItem, maxNumElementSize, enforceMinimal).intValueExact();
                        if (shiftN < 0)
                            throw new ScriptExecutionException(state, "Invalid numer range.");

                        byte[] shifted;
                        switch (opcode) {
                            case OP_LSHIFT:
                                shifted = ScriptUtils.lShift(shiftData, shiftN);
                                break;
                            case OP_RSHIFT:
                                shifted = ScriptUtils.rShift(shiftData, shiftN);
                                break;
                            default:
                                throw new ScriptExecutionException(state, "switched opcode at runtime"); //can't happen
                        }
                        stack.add(shifted, shiftNItem, shiftData);

                        break;
                    case OP_EQUAL:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_EQUAL on a stack with size < 2");
                        StackItem eq2 = stack.pollLast();
                        StackItem eq1 = stack.pollLast();
                        byte[] eqResult = Objects.equals(eq2, eq1) ? new byte[]{1} : new byte[]{};
                        stack.add(eqResult, eq1, eq2);
                        break;
                    case OP_EQUALVERIFY:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_EQUALVERIFY on a stack with size < 2");
                        if (!Objects.equals(stack.pollLast(), stack.pollLast()))
                            throw new ScriptExecutionException(state, "OP_EQUALVERIFY: non-equal data");
                        break;
                    case OP_1ADD:
                    case OP_1SUB:
                    case OP_NEGATE:
                    case OP_ABS:
                    case OP_NOT:
                    case OP_0NOTEQUAL:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted a numeric op on an empty stack");
                        StackItem numericOpItem = stack.pollLast();
                        BigInteger numericOPnum = ScriptUtils.castToBigInteger(state, numericOpItem, maxNumElementSize, enforceMinimal);

                        switch (opcode) {
                            case OP_1ADD:
                                numericOPnum = numericOPnum.add(BigInteger.ONE);
                                break;
                            case OP_1SUB:
                                numericOPnum = numericOPnum.subtract(BigInteger.ONE);
                                break;
                            case OP_NEGATE:
                                numericOPnum = numericOPnum.negate();
                                break;
                            case OP_ABS:
                                if (numericOPnum.signum() < 0)
                                    numericOPnum = numericOPnum.negate();
                                break;
                            case OP_NOT:
                                if (numericOPnum.equals(BigInteger.ZERO))
                                    numericOPnum = BigInteger.ONE;
                                else
                                    numericOPnum = BigInteger.ZERO;
                                break;
                            case OP_0NOTEQUAL:
                                if (numericOPnum.equals(BigInteger.ZERO))
                                    numericOPnum = BigInteger.ZERO;
                                else
                                    numericOPnum = BigInteger.ONE;
                                break;
                            default:
                                throw new AssertionError("Unreachable");
                        }

                        stack.add(ByteTools.reverseBytes(ByteTools.encodeMPI(numericOPnum, false)), numericOpItem);
                        break;
                    case OP_2MUL:
                    case OP_2DIV:
                        throw new ScriptExecutionException(state, "Attempted to use disabled Script Op.");
                    case OP_ADD:
                    case OP_SUB:
                    case OP_DIV:
                    case OP_MUL:
                    case OP_MOD:
                    case OP_BOOLAND:
                    case OP_BOOLOR:
                    case OP_NUMEQUAL:
                    case OP_NUMNOTEQUAL:
                    case OP_LESSTHAN:
                    case OP_GREATERTHAN:
                    case OP_LESSTHANOREQUAL:
                    case OP_GREATERTHANOREQUAL:
                    case OP_MIN:
                    case OP_MAX:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted a numeric op on a stack with size < 2");
                        StackItem numericOpItem2 = stack.pollLast();
                        StackItem numericOpItem1 = stack.pollLast();
                        BigInteger numericOPnum2 = ScriptUtils.castToBigInteger(state, numericOpItem2, maxNumElementSize, enforceMinimal);
                        BigInteger numericOPnum1 = ScriptUtils.castToBigInteger(state, numericOpItem1, maxNumElementSize, enforceMinimal);

                        BigInteger numericOPresult;
                        switch (opcode) {
                            case OP_ADD:
                                numericOPresult = numericOPnum1.add(numericOPnum2);
                                break;
                            case OP_SUB:
                                numericOPresult = numericOPnum1.subtract(numericOPnum2);
                                break;

                            case OP_MUL:
                                numericOPresult = numericOPnum1.multiply(numericOPnum2);
                                break;

                            case OP_DIV:
                                if (numericOPnum2.intValue() == 0)
                                    throw new ScriptExecutionException(state, "Division by zero error");
                                numericOPresult = numericOPnum1.divide(numericOPnum2);
                                break;

                            case OP_MOD:
                                if (numericOPnum2.intValue() == 0)
                                    throw new ScriptExecutionException(state, "Modulo by zero error");

                                /**
                                 * FIXME BigInteger doesn't behave the way we want for modulo operations.  Firstly it's
                                 * always garunteed to return a +ve result.  Secondly it will throw an exception
                                 * if the 2nd operand is negative.
                                 * The remainder method behaves as we expect
                                 */
                                numericOPresult = numericOPnum1.remainder(numericOPnum2);

                                break;

                            case OP_BOOLAND:
                                if (!numericOPnum1.equals(BigInteger.ZERO) && !numericOPnum2.equals(BigInteger.ZERO))
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_BOOLOR:
                                if (!numericOPnum1.equals(BigInteger.ZERO) || !numericOPnum2.equals(BigInteger.ZERO))
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_NUMEQUAL:
                                if (numericOPnum1.equals(numericOPnum2))
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_NUMNOTEQUAL:
                                if (!numericOPnum1.equals(numericOPnum2))
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_LESSTHAN:
                                if (numericOPnum1.compareTo(numericOPnum2) < 0)
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_GREATERTHAN:
                                if (numericOPnum1.compareTo(numericOPnum2) > 0)
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_LESSTHANOREQUAL:
                                if (numericOPnum1.compareTo(numericOPnum2) <= 0)
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_GREATERTHANOREQUAL:
                                if (numericOPnum1.compareTo(numericOPnum2) >= 0)
                                    numericOPresult = BigInteger.ONE;
                                else
                                    numericOPresult = BigInteger.ZERO;
                                break;
                            case OP_MIN:
                                if (numericOPnum1.compareTo(numericOPnum2) < 0)
                                    numericOPresult = numericOPnum1;
                                else
                                    numericOPresult = numericOPnum2;
                                break;
                            case OP_MAX:
                                if (numericOPnum1.compareTo(numericOPnum2) > 0)
                                    numericOPresult = numericOPnum1;
                                else
                                    numericOPresult = numericOPnum2;
                                break;
                            default:
                                throw new RuntimeException("Opcode switched at runtime?");
                        }

                        stack.add(ByteTools.reverseBytes(ByteTools.encodeMPI(numericOPresult, false)), numericOpItem1, numericOpItem2);
                        break;
                    case OP_NUMEQUALVERIFY:
                        if (stack.size() < 2)
                            throw new ScriptExecutionException(state, "Attempted OP_NUMEQUALVERIFY on a stack with size < 2");
                        BigInteger OPNUMEQUALVERIFYnum2 = ScriptUtils.castToBigInteger(state, stack.pollLast(), maxNumElementSize, enforceMinimal);
                        BigInteger OPNUMEQUALVERIFYnum1 = ScriptUtils.castToBigInteger(state, stack.pollLast(), maxNumElementSize, enforceMinimal);

                        if (!OPNUMEQUALVERIFYnum1.equals(OPNUMEQUALVERIFYnum2))
                            throw new ScriptExecutionException(state, "OP_NUMEQUALVERIFY failed");
                        break;
                    case OP_WITHIN:
                        if (stack.size() < 3)
                            throw new ScriptExecutionException(state, "Attempted OP_WITHIN on a stack with size < 3");
                        StackItem OPWITHINitem3 = stack.pollLast();
                        StackItem OPWITHINitem2 = stack.pollLast();
                        StackItem OPWITHINitem1 = stack.pollLast();
                        BigInteger OPWITHINnum3 = ScriptUtils.castToBigInteger(state, OPWITHINitem3, maxNumElementSize, enforceMinimal);
                        BigInteger OPWITHINnum2 = ScriptUtils.castToBigInteger(state, OPWITHINitem2, maxNumElementSize, enforceMinimal);
                        BigInteger OPWITHINnum1 = ScriptUtils.castToBigInteger(state, OPWITHINitem1, maxNumElementSize, enforceMinimal);
                        byte[] OPWITHINresult;
                        if (OPWITHINnum2.compareTo(OPWITHINnum1) <= 0 && OPWITHINnum1.compareTo(OPWITHINnum3) < 0)
                            OPWITHINresult = ByteTools.encodeMPI(BigInteger.ONE, false);
                        else
                            OPWITHINresult = ByteTools.encodeMPI(BigInteger.ZERO, false);
                        stack.add(ByteTools.reverseBytes(OPWITHINresult), OPWITHINitem1, OPWITHINitem2, OPWITHINitem3);
                        break;
                    case OP_RIPEMD160:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_RIPEMD160 on an empty stack");
                        RIPEMD160Digest digest = new RIPEMD160Digest();
                        StackItem r160data = stack.pollLast();
                        digest.update(r160data.bytes(), 0, r160data.length());
                        byte[] ripmemdHash = new byte[20];
                        digest.doFinal(ripmemdHash, 0);
                        stack.add(ripmemdHash, r160data);
                        break;
                    case OP_SHA1:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA1 on an empty stack");
                        try {
                            StackItem sha1Data = stack.pollLast();
                            stack.add(MessageDigest.getInstance("SHA-1").digest(sha1Data.bytes()), sha1Data);
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);  // Cannot happen.
                        }
                        break;
                    case OP_SHA256:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA256 on an empty stack");
                        StackItem sha256Data = stack.pollLast();
                        stack.add(Sha256.hash(sha256Data.bytes()), sha256Data);
                        break;
                    case OP_HASH160:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_HASH160 on an empty stack");
                        StackItem hash160Data = stack.pollLast();
                        stack.add(Sha256.sha256hash160(hash160Data.bytes()), hash160Data);
                        break;
                    case OP_HASH256:
                        if (stack.size() < 1)
                            throw new ScriptExecutionException(state, "Attempted OP_SHA256 on an empty stack");
                        StackItem hash256Data = stack.pollLast();
                        stack.add(Sha256.hashTwice(hash256Data.bytes()), hash256Data);
                        break;
                    case OP_CODESEPARATOR:
                        lastCodeSepLocation = chunk.getStartLocationInProgram() + 1;
                        break;
                    case OP_CHECKSIG:
                    case OP_CHECKSIGVERIFY:
                        if (txContainingThis == null)
                            throw new IllegalStateException("Script attempted signature check but no tx was provided");
                        if (scriptConfig.isDummySignaturesAllowed()) {

                        } else {
                            executeCheckSig(state, txContainingThis, (int) index, script, stack, lastCodeSepLocation, opcode, value, scriptConfig.isDummySignaturesAllowed());
                        }
                        break;
                    case OP_CHECKMULTISIG:
                    case OP_CHECKMULTISIGVERIFY:
                        if (txContainingThis == null)
                            throw new IllegalStateException("Script attempted signature check but no tx was provided");
                        opCount = executeMultiSig(state, txContainingThis, (int) index, script, stack, opCount, maxOpCount, maxMultisigKeys, lastCodeSepLocation, opcode, value, scriptConfig.isDummySignaturesAllowed());
                        state.opCount = opCount;
                        break;
                    case OP_CHECKLOCKTIMEVERIFY:
                        if (!scriptConfig.isCheckLockTimeVerifyEnabled() || !scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.CHECKLOCKTIMEVERIFY)) {
                            // not enabled; treat as a NOP2
                            if (scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                                throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                            }
                            break;
                        }
                        executeCheckLockTimeVerify(state, txContainingThis, (int) index, stack, lastCodeSepLocation, opcode);
                        break;
                    case OP_NOP1:
                    case OP_NOP3:
                    case OP_NOP4:
                    case OP_NOP5:
                    case OP_NOP6:
                    case OP_NOP7:
                    case OP_NOP8:
                    case OP_NOP9:
                    case OP_NOP10:
                        if (scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.DISCOURAGE_UPGRADABLE_NOPS)) {
                            throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                        }
                        break;

                    default:
                        throw new ScriptExecutionException(state, "Script used a reserved opcode " + opcode);
                }
            }

            // If enabled, we check the stack size in terms of number of elements:
            if (scriptConfig.getMaxStackNumElements().isPresent()) {
                if (stack.size() + altstack.size() > scriptConfig.getMaxStackNumElements().get() || stack.size() + altstack.size() < 0)
                    throw new ScriptExecutionException(state, "Stack size exceeded range");
            }
            // If enabled, we check the stack size in terms of size (bytes):
            if (scriptConfig.getMaxStackSizeInBytes().isPresent()) {
                long stackBytes = stack.getStackMemoryUsage() + altstack.getStackMemoryUsage();
                if (stackBytes > scriptConfig.getMaxStackSizeInBytes().get())
                    throw new ScriptExecutionException(state, "Stack memory usage consensus exceeded");
            }

            if (scriptStateListener != null) {
                scriptStateListener.onAfterOpCodeExectuted();
            }

            if (opReturnCalled) {
                break;
            }

        }

        if (!ifStack.isEmpty())
            throw new ScriptExecutionException(state, "OP_IF/OP_NOTIF without OP_ENDIF");

        if (scriptStateListener != null) {
            scriptStateListener.onScriptComplete();
        }

        // We return the result of the Script
        ScriptResult result = ScriptResult.builder().stack(stack).build();
        return result;
    }

    // This is more or less a direct translation of the code in Bitcoin Core
    private void executeCheckLockTimeVerify(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStack stack,
                                                   int lastCodeSepLocation, int opcode) throws ScriptExecutionException {
        if (stack.size() < 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKLOCKTIMEVERIFY on a stack with size < 1");

        // Thus as a special case we tell CScriptNum to accept up
        // to 5-byte bignums to avoid year 2038 issue.
        StackItem nLockTimeItem = stack.getLast();
        //we don't modify the stack so no need to worry about passing on derivation status of stack items.
        final BigInteger nLockTime = ScriptUtils.castToBigInteger(state, nLockTimeItem, 5, state.verifyFlags.contains(ScriptVerifyFlag.MINIMALDATA));

        if (nLockTime.compareTo(BigInteger.ZERO) < 0)
            throw new ScriptExecutionException(state, "Negative locktime");

        // There are two kinds of nLockTime, need to ensure we're comparing apples-to-apples
        if (!(
                ((txContainingThis.getLockTime() < JCLScriptConstants.LOCKTIME_THRESHOLD)
                        && (nLockTime.compareTo(BigInteger.valueOf(JCLScriptConstants.LOCKTIME_THRESHOLD)) < 0))  ||
                        ((txContainingThis.getLockTime() >= JCLScriptConstants.LOCKTIME_THRESHOLD)
                                && (nLockTime.compareTo(BigInteger.valueOf(JCLScriptConstants.LOCKTIME_THRESHOLD))) >= 0))
        )
            throw new ScriptExecutionException(state, "Locktime requirement type mismatch");

        // Now that we know we're comparing apples-to-apples, the
        // comparison is a simple numeric one.
        if (nLockTime.compareTo(BigInteger.valueOf(txContainingThis.getLockTime())) > 0)
            throw new ScriptExecutionException(state, "Locktime requirement not satisfied");

        // Finally the nLockTime feature can be disabled and thus
        // CHECKLOCKTIMEVERIFY bypassed if every txin has been
        // finalized by setting nSequence to maxint. The
        // transaction would be allowed into the blockchain, making
        // the opcode ineffective.
        //
        // Testing if this vin is not final is sufficient to
        // prevent this condition. Alternatively we could test all
        // inputs, but testing just this input minimizes the data
        // required to prove correct CHECKLOCKTIMEVERIFY execution.
        if (!txContainingThis.getInputs().get(index).hasSequence())
            throw new ScriptExecutionException(state, "Transaction contains a final transaction input for a CHECKLOCKTIMEVERIFY script.");
    }

    private static void executeCheckSig(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                        int lastCodeSepLocation, int opcode, Coin value,
                                        boolean allowFakeChecksig) throws ScriptExecutionException {

        final boolean requireCanonical = !allowFakeChecksig &&
                (state.verifyFlags.contains(ScriptVerifyFlag.STRICTENC)
                        || state.verifyFlags.contains(ScriptVerifyFlag.DERSIG)
                        || state.verifyFlags.contains(ScriptVerifyFlag.LOW_S));

        if (stack.size() < 2)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKSIG(VERIFY) on a stack with size < 2");
        StackItem pubKey = stack.pollLast();
        StackItem sigBytes = stack.pollLast();

        boolean sigValid = false;

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sigBytes.length() + 1);
        ScriptChunkSerializer.getInstance().serialize(null, sigBytes.bytes(), outStream);
        connectedScript = SigHash.removeAllInstancesOf(connectedScript, outStream.toByteArray());

        // TODO: Use int for indexes everywhere, we can't have that many inputs/outputs
        try {
            TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigBytes.bytes(), requireCanonical,
                    state.verifyFlags.contains(ScriptVerifyFlag.LOW_S));

            // TODO: Should check hash type is known
            Sha256Wrapper hash = sig.useForkId() ?
                    SigHash.hashForForkIdSignature(txContainingThis, index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()) :
                    SigHash.hashForLegacySignature(txContainingThis, index, connectedScript, (byte) sig.sighashFlags);
            sigValid = allowFakeChecksig ? true : ECDSA.verify(hash.getBytes(), sig, pubKey.bytes());
        } catch (Exception e1) {
            // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
            // Because I can't verify there aren't more, we use a very generic Exception catch

            // This RuntimeException occurs when signing as we run partial/invalid scripts to see if they need more
            // signing work to be done inside LocalTransactionSigner.signInputs.
            if (!e1.getMessage().contains("Reached past end of ASN.1 stream"))
                log.warn("Signature checking failed!", e1);
        }


        if (opcode == OP_CHECKSIG)
            stack.add(sigValid ? new byte[]{1} : new byte[]{}, pubKey, sigBytes);
        else if (opcode == OP_CHECKSIGVERIFY)
            if (!sigValid)
                throw new ScriptExecutionException(state, "Script failed OP_CHECKSIGVERIFY");
    }

    private int executeMultiSig(ScriptExecutionState state, Tx txContainingThis, int index, ScriptStream script, ScriptStack stack,
                                       int opCount, int maxOpCount, int maxKeys, int lastCodeSepLocation, int opcode, Coin value,
                                       boolean allowFakeChecksig) throws ScriptExecutionException {
        final boolean requireCanonical = !allowFakeChecksig &&
                (state.verifyFlags.contains(ScriptVerifyFlag.STRICTENC)
                        || state.verifyFlags.contains(ScriptVerifyFlag.DERSIG)
                        || state.verifyFlags.contains(ScriptVerifyFlag.LOW_S));

        final boolean enforceMinimal = !allowFakeChecksig && state.verifyFlags.contains(ScriptVerifyFlag.MINIMALDATA);
        if (stack.size() < 2)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < 2");

        List<StackItem> polledStackItems = new LinkedList<>();

        StackItem pubKeyCountItem = stack.pollLast();
        polledStackItems.add(pubKeyCountItem);

        //we'll allow the highest possible pubKeyCount as it's immediately check after and this ensures
        //we get a meaningful error message
        int pubKeyCount = ScriptUtils.castToBigInteger(state, pubKeyCountItem, state.config.getMaxNumberSizeInBytes(), enforceMinimal).intValue();
        if (pubKeyCount < 0 || pubKeyCount > maxKeys)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with pubkey count out of range");
        opCount += pubKeyCount;
        if (opCount > maxOpCount)
            throw new ScriptExecutionException(state, "Total op count > " + maxOpCount + " during OP_CHECKMULTISIG(VERIFY)");
        if (stack.size() < pubKeyCount + 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + 2");


        LinkedList<StackItem> pubkeys = new LinkedList<>();
        for (int i = 0; i < pubKeyCount; i++) {
            StackItem pubKey = stack.pollLast();
            pubkeys.add(pubKey);
        }
        polledStackItems.addAll(pubkeys);

        StackItem sigCountItem = stack.pollLast();
        polledStackItems.add(sigCountItem);
        int sigCount = ScriptUtils.castToBigInteger(state, sigCountItem, maxKeys, enforceMinimal).intValue();
        if (sigCount < 0 || sigCount > pubKeyCount)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with sig count out of range");
        if (stack.size() < sigCount + 1)
            throw new ScriptExecutionException(state, "Attempted OP_CHECKMULTISIG(VERIFY) on a stack with size < num_of_pubkeys + num_of_signatures + 3");

        LinkedList<StackItem> sigs = new LinkedList<>();
        for (int i = 0; i < sigCount; i++) {
            StackItem sig = stack.pollLast();
            sigs.add(sig);
        }
        polledStackItems.addAll(sigs);

        byte[] connectedScript = script.getProgramFrom(script.getLastCodeSepIndex());

        for (StackItem sig : sigs) {
            UnsafeByteArrayOutputStream outStream = new UnsafeByteArrayOutputStream(sig.length() + 1);
            ScriptChunkSerializer.getInstance().serialize(null, sig.bytes(), outStream);
            connectedScript = SigHash.removeAllInstancesOf(connectedScript, outStream.toByteArray());
        }

        boolean valid = true;
        while (sigs.size() > 0) {
            StackItem pubKey = pubkeys.pollFirst();
            // We could reasonably move this out of the loop, but because signature verification is significantly
            // more expensive than hashing, its not a big deal.
            try {
                TransactionSignature sig = TransactionSignature.decodeFromBitcoin(sigs.getFirst().bytes(), requireCanonical,
                        state.verifyFlags.contains(ScriptVerifyFlag.LOW_S));
                Sha256Wrapper hash = sig.useForkId() ?
                        SigHash.hashForForkIdSignature(txContainingThis, index, connectedScript, value, sig.sigHashMode(), sig.anyoneCanPay()) :
                        SigHash.hashForLegacySignature(txContainingThis, index, connectedScript, (byte) sig.sighashFlags);
                if (allowFakeChecksig || ECDSA.verify(hash.getBytes(), sig, pubKey.bytes()))
                    sigs.pollFirst();
            } catch (Exception e) {
                // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
                // Because I can't verify there aren't more, we use a very generic Exception catch
            }

            if (sigs.size() > pubkeys.size()) {
                valid = false;
                break;
            }
        }

        // We uselessly remove a stack object to emulate a Bitcoin Core bug.
        StackItem nullDummy = stack.pollLast();
        //this could have been provided in scriptSig so still has an impact on whether the result is derived
        polledStackItems.add(nullDummy);
        if (state.verifyFlags.contains(ScriptVerifyFlag.NULLDUMMY) && nullDummy.length() > 0)
            throw new ScriptExecutionException(state, "OP_CHECKMULTISIG(VERIFY) with non-null nulldummy: " + Arrays.toString(nullDummy.bytes()));

        if (opcode == OP_CHECKMULTISIG) {
            StackItem[] polledItems = polledStackItems.toArray(new StackItem[polledStackItems.size()]);
            stack.add(valid ? new byte[]{1} : new byte[]{}, polledItems);
        } else if (opcode == OP_CHECKMULTISIGVERIFY) {
            if (!valid)
                throw new ScriptExecutionException(state, "Script failed OP_CHECKMULTISIGVERIFY");
        }
        return opCount;
    }



    /**
     * Verifies that this script (interpreted as a scriptSig) correctly spends the given scriptPubKey.
     *
     * @param scriptConfig          Script Configuration to apply to the Script execution
     * @param scriptSig             Unlocking Script
     * @param scriptPubKey          Locking Script. The script containing the conditions needed to claim the value.
     */
    public ScriptResult execute(ScriptConfig scriptConfig,
                                ScriptContext scriptContext,
                                Script scriptSig,
                                Script scriptPubKey) throws ScriptExecutionException {

        ScriptResult result = null;

        if (scriptSig.getProgram().length > 10000 || scriptPubKey.getProgram().length > 10000)
            throw new ScriptExecutionException("Script larger than 10,000 bytes");

        ScriptStack stack = new ScriptStack();
        ScriptStack p2shStack = null;

        Coin value = scriptContext.getValue();

        // First we run the Unlocking Script (ScriptSig):
        ScriptResult resultScriptSig = execute(scriptConfig, scriptContext, scriptSig);

        if (scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.P2SH))
            p2shStack = new ScriptStack(resultScriptSig.getStack());

        // Now we run the Locking Script (scriptPubKey):
        // We need to feed the Stack from the previous execution into this one:
        scriptContext = scriptContext.toBuilder().stack(resultScriptSig.getStack()).build();
        ScriptResult resultScriptPubKey = execute(scriptConfig, scriptContext, scriptPubKey);

        if (resultScriptPubKey.getStack().size() == 0)
            throw new ScriptExecutionException("Stack empty at end of script execution.");

        if (!ScriptUtils.castToBool(resultScriptPubKey.getStack().pollLast().bytes()))
            throw new ScriptExecutionException("Script resulted in a non-true stack: " + stack);

        result = resultScriptPubKey;

        // P2SH is pay to script hash. It means that the scriptPubKey has a special form which is a valid
        // program but it has "useless" form that if evaluated as a normal program always returns true.
        // Instead, miners recognize it as special based on its template - it provides a hash of the real scriptPubKey
        // and that must be provided by the input. The goal of this bizarre arrangement is twofold:
        //
        // (1) You can sum up a large, complex script (like a CHECKMULTISIG script) with an address that's the same
        //     size as a regular address. This means it doesn't overload scannable QR codes/NFC tags or become
        //     un-wieldy to copy/paste.
        // (2) It allows the working set to be smaller: nodes perform best when they can store as many unspent outputs
        //     in RAM as possible, so if the outputs are made smaller and the inputs get bigger, then it's better for
        //     overall scalability and performance.

        // TODO: Check if we can take out enforceP2SH if there's a checkpoint at the enforcement block.
        if (scriptConfig.getVerifyFlags().contains(ScriptVerifyFlag.P2SH) && scriptPubKey.isPayToScriptHash()) {
            for (ScriptChunk chunk : scriptSig.getChunks())
                if (chunk.isOpCode() && chunk.getOpcode() > OP_16)
                    throw new ScriptExecutionException("Attempted to spend a P2SH scriptPubKey with a script that contained script ops");

            StackItem scriptPubKeyBytes = p2shStack.pollLast();
            Script scriptPubKeyP2SH = Script.builder(scriptPubKeyBytes.bytes()).build();
            ScriptContext scriptP2HContext = scriptContext.toBuilder().stack(p2shStack).build();

            ScriptResult resultP2SHScript = null;
            resultP2SHScript = execute(scriptConfig, scriptP2HContext,  scriptPubKeyP2SH);

            if (resultP2SHScript.getStack().size() == 0)
                throw new ScriptExecutionException("P2SH stack empty at end of script execution.");

            if (!ScriptUtils.castToBool(resultP2SHScript.getStack().pollLast().bytes()))
                throw new ScriptExecutionException("P2SH script execution resulted in a non-true stack");

            result = resultP2SHScript;
        }

        return result;
    }
}
