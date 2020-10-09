package com.nchain.jcl.script.config;

import com.nchain.jcl.script.core.ScriptVerifyFlag;
import lombok.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.nchain.jcl.script.core.ScriptOpCodes.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Getter
@NoArgsConstructor
public class ScriptConfigImpl implements ScriptConfig {
    protected long                    maxDataSizeInBytes;
    protected int                     maxNumberSizeInBytes;
    protected int                     maxNumMultisigPubkeys;
    protected int                     maxOpCount;
    protected Optional<Integer>       maxStackNumElements;
    protected Optional<Long>          maxStackSizeInBytes;
    protected boolean                 exceptionThrownOnOpReturn;
    protected boolean                 checkLockTimeVerifyEnabled;
    protected boolean                 dummySignaturesAllowed;
    protected int                     addressHeader;
    protected int                     p2shHeader;
    protected Set<ScriptVerifyFlag>   verifyFlags;

    @Builder(toBuilder = true)
    public ScriptConfigImpl(long maxDataSizeInBytes,
                            int maxNumberSizeInBytes,
                            int maxNumMultisigPubkeys,
                            int maxOpCount,
                            Optional<Integer> maxStackNumElements,
                            Optional<Long> maxStackSizeInBytes,
                            boolean exceptionThrownOnOpReturn,
                            boolean checkLockTimeVerifyEnabled,
                            boolean dummySignaturesAllowed,
                            int addressHeader,
                            int p2shHeader,
                            Set<ScriptVerifyFlag> verifyFlags) {
        this.maxDataSizeInBytes = maxDataSizeInBytes;
        this.maxNumberSizeInBytes = maxNumberSizeInBytes;
        this.maxNumMultisigPubkeys = maxNumMultisigPubkeys;
        this.maxOpCount = maxOpCount;
        this.maxStackNumElements = maxStackNumElements;
        this.maxStackSizeInBytes = maxStackSizeInBytes;
        this.exceptionThrownOnOpReturn = exceptionThrownOnOpReturn;
        this.checkLockTimeVerifyEnabled = checkLockTimeVerifyEnabled;
        this.dummySignaturesAllowed = dummySignaturesAllowed;
        this.addressHeader = addressHeader;
        this.p2shHeader = p2shHeader;

        // Defensive copy to enforce immutability:
        this.verifyFlags = (verifyFlags == null) ? null : Collections.unmodifiableSet(new HashSet<>(verifyFlags));
    }

    /** Default implementation of the criteria to determine whether an opCode is Disabled */
    @Override
    public boolean isOpCodeDisabled(int opCode) {
        switch (opCode) {

            case OP_2MUL:
            case OP_2DIV:
                //disabled codes
                return true;

            case OP_INVERT:
            case OP_LSHIFT:
            case OP_RSHIFT:
            case OP_MUL:
                //enabled codes, still disabled if flag is not activated
                return !verifyFlags.contains(ScriptVerifyFlag.MAGNETIC_OPCODES);

            case OP_CAT:
            case OP_SPLIT:
            case OP_AND:
            case OP_OR:
            case OP_XOR:
            case OP_DIV:
            case OP_MOD:
            case OP_NUM2BIN:
            case OP_BIN2NUM:
                //enabled codes, still disabled if flag is not activated
                return !verifyFlags.contains(ScriptVerifyFlag.MONOLITH_OPCODES);

            default:
                //not an opcode that was ever disabled
                break;
        }
        return false;
    }
}
