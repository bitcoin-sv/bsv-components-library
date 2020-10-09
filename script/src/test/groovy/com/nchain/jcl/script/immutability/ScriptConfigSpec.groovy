package com.nchain.jcl.script.immutability

import com.nchain.jcl.script.config.ScriptConfig
import com.nchain.jcl.script.config.provided.ScriptBSVGenesisConfig
import com.nchain.jcl.script.core.ScriptVerifyFlag
import static com.nchain.jcl.script.core.ScriptVerifyFlag.*;
import spock.lang.Specification

class ScriptConfigSpec extends Specification {
    def "creating instance and change original values"() {
        given:
            Set<ScriptVerifyFlag> flags = new HashSet<>()
            flags.add(P2SH)
            flags.add(STRICTENC)
            ScriptConfig scriptConfig = new ScriptBSVGenesisConfig().toBuilder()
                .verifyFlags(flags)
                .build()
        when:
            flags.clear()
        then:
            scriptConfig.getVerifyFlags().size() != flags.size()
    }

    def "getting values from instance and change them"() {
        given:
            Set<ScriptVerifyFlag> flags = new HashSet<>()
            flags.add(P2SH)
            flags.add(STRICTENC)
            ScriptConfig scriptConfig = new ScriptBSVGenesisConfig().toBuilder()
                    .verifyFlags(flags)
                    .build()
        when:
            scriptConfig.getVerifyFlags().clear()
        then:
            thrown UnsupportedOperationException
    }
}
