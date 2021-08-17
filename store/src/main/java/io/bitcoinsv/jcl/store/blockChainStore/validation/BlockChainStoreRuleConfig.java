package io.bitcoinsv.jcl.store.blockChainStore.validation;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStoreConfig;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.BlockChainRule;

import java.util.ArrayList;
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

    public static BlockChainStoreLevelDBConfigBuilder builder() {
        return new BlockChainStoreLevelDBConfigBuilder();
    }

    public  List<BlockChainRule> getRuleList() { return ruleList;}
    public static class BlockChainStoreLevelDBConfigBuilder {

            List<BlockChainRule> ruleList = new ArrayList<>();

            public BlockChainStoreRuleConfig.BlockChainStoreLevelDBConfigBuilder addRule(BlockChainRule rule) {
                ruleList.add(rule);
                return this;
            }

            public BlockChainStoreRuleConfig build() {
                return new BlockChainStoreRuleConfig(ruleList);
            }
        }


}
