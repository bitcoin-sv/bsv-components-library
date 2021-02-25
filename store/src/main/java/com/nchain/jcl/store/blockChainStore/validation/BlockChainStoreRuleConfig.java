package com.nchain.jcl.store.blockChainStore.validation;

import com.nchain.jcl.store.blockChainStore.BlockChainStoreConfig;
import com.nchain.jcl.store.blockChainStore.validation.rules.*;

import java.util.List;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public class BlockChainStoreRuleConfig implements BlockChainStoreConfig {

    List<BlockChainRule> ruleList;

    public BlockChainStoreRuleConfig(List<BlockChainRule> ruleList) {
        this.ruleList = ruleList;
    }

    public  List<BlockChainRule> getRuleList() { return ruleList;}
    public class BlockChainStoreLevelDBConfigBuilder {

            List<BlockChainRule> ruleList;

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withNewDifficultyAdjustmentAlgorithmRule(NewDifficultyAdjustmentAlgorithmRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withMinimalDifficultyRule(MinimalDifficultyRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withMinimalDifficultyNoChangedRule(MinimalDifficultyNoChangedRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withLastNonMinimalDifficultyRuleRule(LastNonMinimalDifficultyRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withEmergencyDifficultyAdjustmentRuleRule(EmergencyDifficultyAdjustmentRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder withDifficultyTransitionPointRuleRule(DifficultyTransitionPointRule rule) {
                ruleList.add(rule);
                return this;
            }


            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder addRule(BlockChainRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig build() {
                return new BlockChainStoreRuleConfig(ruleList);
            }
        }


}
