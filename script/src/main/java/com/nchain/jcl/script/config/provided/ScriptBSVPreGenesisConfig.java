package com.nchain.jcl.script.config.provided;

import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.config.ScriptConfigImpl;
import com.nchain.jcl.script.core.ScriptVerifyFlag;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Getter
public class ScriptBSVPreGenesisConfig extends ScriptConfigImpl implements ScriptConfig {
    private static final long                  maxDataSizeInBytes = 520;
    private static final int                   maxNumberSizeInBytes = 4;
    private static final int                   maxNumMultisigPubkeys = 20;
    private static final int                   maxOpCount = 500;
    private static final Optional<Integer>     maxStackNumElements = Optional.of(1000);
    private static final Optional<Long>        maxStackSizeInBytes = Optional.empty();
    private static final boolean               exceptionThrownOnOpReturn = true;
    private static final boolean               checkLockTimeVerifyEnabled = true;
    private static final boolean               dummySignaturesAllowed = false;
    private static final int                   addressHeader = 0;
    private static final int                   p2shHeader = 5;
    private static final Set<ScriptVerifyFlag> verifyFlags = Set.of(ScriptVerifyFlag.MAGNETIC_OPCODES);

    public ScriptBSVPreGenesisConfig() {
        super(
                maxDataSizeInBytes,
                maxNumberSizeInBytes,
                maxNumMultisigPubkeys,
                maxOpCount,
                maxStackNumElements,
                maxStackSizeInBytes,
                exceptionThrownOnOpReturn,
                checkLockTimeVerifyEnabled,
                dummySignaturesAllowed,
                addressHeader,
                p2shHeader,
                verifyFlags
        );
    }
}
