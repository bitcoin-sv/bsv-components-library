package com.nchain.jcl.store.blockChainStore.validation;


import com.nchain.jcl.store.blockChainStore.BlockChainStore;
import com.nchain.jcl.store.blockChainStore.validation.rules.*;
import com.nchain.jcl.store.blockChainStore.validation.rules.predicate.DifficultyAdjustmentActivatedPredicate;
import com.nchain.jcl.store.blockChainStore.validation.rules.predicate.DifficultyEqualtoMaxTargetPredicate;
import com.nchain.jcl.store.blockChainStore.validation.rules.predicate.DifficultyTransitionPointPredicate;
import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.params.NetworkParameters;

import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-03
 *
 * This class takes a NetworkParams class from bitcoinJ and generates a BlockChainStoreConfig
 */
public class RuleConfigBuilder {


    public static BlockChainStoreRuleConfig get(NetworkParameters params, BlockChainStore blockChainStore) {

        BlockChainStoreRuleConfig blockChainStoreRuleConfig;

        switch(params.getId()) {
            case "org.bitcoin.production":
                Predicate<ChainInfo> newDifficultyAdjustmentAlgorithmRulePredicate = new DifficultyAdjustmentActivatedPredicate(params.getDAAUpdateHeight());
                Predicate<ChainInfo> difficultyTransitionPointPredicate = new DifficultyTransitionPointPredicate(params.getInterval());
                Predicate<ChainInfo> blockDifficultyEqualToMaxTarget = new DifficultyEqualtoMaxTargetPredicate(params.getMaxTarget());


                BlockChainRule newDifficultyAdjustmentAlgorithmRule = new NewDifficultyAdjustmentAlgorithmRule(newDifficultyAdjustmentAlgorithmRulePredicate,
                        blockChainStore,
                        params.getMaxTarget(),
                        params.getInterval());

                BlockChainRule difficultyTransitionPointRule = new DifficultyTransitionPointRule(difficultyTransitionPointPredicate,
                        blockChainStore,
                        params.getMaxTarget(),
                        params.getInterval(),
                        params.getTargetTimespan());

                BlockChainRule minimalDifficultyNoChangedRule = new MinimalDifficultyNoChangedRule(difficultyTransitionPointPredicate.negate().and(blockDifficultyEqualToMaxTarget),
                        blockChainStore,
                        params.getMaxTarget());


                BlockChainRule emergencyDifficultyAdjustmentRule = new EmergencyDifficultyAdjustmentRule(difficultyTransitionPointPredicate.negate().and(blockDifficultyEqualToMaxTarget.negate()),
                        blockChainStore,
                        params.getMaxTarget());

                blockChainStoreRuleConfig = BlockChainStoreRuleConfig.builder()
                        .addRule(newDifficultyAdjustmentAlgorithmRule)
                        .addRule(difficultyTransitionPointRule)
                        .addRule(minimalDifficultyNoChangedRule)
                        .addRule(emergencyDifficultyAdjustmentRule)
                        .build();

                break;

            case "org.bitcoin.testnet":
            case "org.bitcoin.stn":
            default:
                throw new UnsupportedOperationException();

        }


        return blockChainStoreRuleConfig;
    }

}
