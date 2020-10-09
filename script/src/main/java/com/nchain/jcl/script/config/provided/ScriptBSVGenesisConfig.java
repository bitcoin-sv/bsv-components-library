package com.nchain.jcl.script.config.provided;

import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.config.ScriptConfigImpl;
import com.nchain.jcl.script.core.ScriptVerifyFlag;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

import javax.script.ScriptContext;
import java.util.*;

import static com.nchain.jcl.script.core.ScriptOpCodes.OP_2DIV;
import static com.nchain.jcl.script.core.ScriptOpCodes.OP_2MUL;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Getter
public class ScriptBSVGenesisConfig  extends ScriptConfigImpl implements ScriptConfig {
    private static final long                  maxDataSizeInBytes = Long.MAX_VALUE;
    private static final int                   maxNumberSizeInBytes = 750000;
    private static final int                   maxNumMultisigPubkeys = Integer.MAX_VALUE;
    private static final int                   maxOpCount = Integer.MAX_VALUE;
    private static final Optional<Integer>     maxStackNumElements = Optional.empty();
    private static final Optional<Long>        maxStackSizeInBytes = Optional.of((long) 100 * 1000 * 1000);
    private static final boolean               exceptionThrownOnOpReturn = false;
    private static final boolean               checkLockTimeVerifyEnabled = false;
    private static final boolean               dummySignaturesAllowed = false;
    private static final int                   addressHeader = 0;
    private static final int                   p2shHeader = 5;
    private static final Set<ScriptVerifyFlag> verifyFlags = Set.of(ScriptVerifyFlag.GENESIS_OPCODES);

    public ScriptBSVGenesisConfig() {
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

