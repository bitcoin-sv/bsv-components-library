package com.nchain.jcl.script.config.provided;

import com.nchain.jcl.script.config.ScriptConfig;
import com.nchain.jcl.script.config.ScriptConfigImpl;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Getter
public class ScriptBSVPreMagneticConfig extends ScriptConfigImpl implements ScriptConfig {
    private static final ScriptBSVPreGenesisConfig preGenesisConfig = new ScriptBSVPreGenesisConfig();
    private static final int maxOpCount = 201;

    public ScriptBSVPreMagneticConfig() {
        super(
                preGenesisConfig.getMaxDataSizeInBytes(),
                preGenesisConfig.getMaxNumberSizeInBytes(),
                preGenesisConfig.getMaxNumMultisigPubkeys(),
                maxOpCount,
                preGenesisConfig.getMaxStackNumElements(),
                preGenesisConfig.getMaxStackSizeInBytes(),
                preGenesisConfig.isExceptionThrownOnOpReturn(),
                preGenesisConfig.isCheckLockTimeVerifyEnabled(),
                preGenesisConfig.isDummySignaturesAllowed(),
                preGenesisConfig.getAddressHeader(),
                preGenesisConfig.getP2shHeader(),
                preGenesisConfig.getVerifyFlags()
        );
    }
}
