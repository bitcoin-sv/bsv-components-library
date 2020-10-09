package com.nchain.jcl.script.config;

import com.nchain.jcl.script.config.provided.ScriptBSVGenesisConfig;
import com.nchain.jcl.script.config.provided.ScriptBSVPreGenesisConfig;
import com.nchain.jcl.script.config.provided.ScriptBSVPreMagneticConfig;
import com.nchain.jcl.script.core.ScriptVerifyFlag;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Script Configuration stors information about the RULEs that we want to enforce when running a Script. The same
 * Script, under dofferent Configurations, might result in different execution results. These rules are part of the
 * Bitcoin Consensus mechanism, and they determine whether the result of a Script is Correct or not, therefore they
 * determine whether the Transactions are accepted and included in a Block, or rejectd and discarded.
 */
public interface ScriptConfig {

    /** Maximum Size (in bytes) of the DATA pushed by a Script operator (data size of a ScriptChunk) */
    long getMaxDataSizeInBytes();

    /** Maximum size (in bytes) of the numbers stored in the Stack during the Script execution */
    int getMaxNumberSizeInBytes();

    /** Maximum number of Signatures in a Multi-sig Script */
    int getMaxNumMultisigPubkeys();

    /** Maximum number of Op Codes (higher than OP_16) in the Script */
    int getMaxOpCount();

    /** If present, then the Stack cannot have more elements than this number, or an exception will be thrown */
    Optional<Integer> getMaxStackNumElements();

    /** If present, then the size (in bytes) taken by all the elements in the Stack cannot be higher than than this number, or an exception will be thrown */
    Optional<Long> getMaxStackSizeInBytes();

    /** IF TRUE, and exception is thrown on a OP_RETURN, else Script execution is just halted */
    boolean isExceptionThrownOnOpReturn();

    /** If TRUE, the CHECKLOCKTIMEVERIFY OpCode is executed, else it's ignored */
    boolean isCheckLockTimeVerifyEnabled();

    /** If TRUE, then OP_CHECKSIGVERIFY is not executed */
    boolean isDummySignaturesAllowed();

    /** It represents the "Header" used in all the Bitcoin Addresses */
    int getAddressHeader();

    /** It represents the "Header" of a P2H Address */
    int getP2shHeader();

    /** List of Verification Flags used to run the script */
    Set<ScriptVerifyFlag> getVerifyFlags();

    /** Indicates if the opCode given is disabled or not */
    boolean isOpCodeDisabled(int opCode);

    // Convenience method to get a reference to the Builder
    static ScriptConfigImpl.ScriptConfigImplBuilder builder() { return ScriptConfigImpl.builder();}

    // Convenience method to create an instance of ScriptConfig based on a list of Verification Flags
    static ScriptConfigImpl.ScriptConfigImplBuilder builder(Set<ScriptVerifyFlag> flags) {
        ScriptConfigImpl.ScriptConfigImplBuilder result = new ScriptBSVGenesisConfig().toBuilder();

        if (!flags.contains(ScriptVerifyFlag.GENESIS_OPCODES))
            if (flags.contains(ScriptVerifyFlag.MAGNETIC_OPCODES))
                result = new ScriptBSVPreGenesisConfig().toBuilder();
            else    result = new ScriptBSVPreMagneticConfig().toBuilder();
        result.verifyFlags(flags);
        return result;
    }
}
