/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.blockChainStore.validation;


import io.bitcoinj.bitcoin.api.extended.ChainInfo;
import io.bitcoinj.params.NetworkParameters;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.*;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate.DifficultyAdjustmentActivatedPredicate;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate.DifficultyEqualtoMaxTargetPredicate;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate.DifficultyTransitionPointPredicate;
import io.bitcoinsv.jcl.store.blockChainStore.validation.rules.predicate.GenesisPredicate;

import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-02-03
 *
 * This class takes a NetworkParams class from bitcoinJ and generates a BlockChainStoreConfig
 */
public class RuleConfigBuilder {


    public static BlockChainStoreRuleConfig get(NetworkParameters params) {

        BlockChainStoreRuleConfig blockChainStoreRuleConfig;

        Predicate<ChainInfo> difficultyAdjustmentActivatedPredicate = new DifficultyAdjustmentActivatedPredicate(params.getDAAUpdateHeight());
        Predicate<ChainInfo> difficultyTransitionPointPredicate = new DifficultyTransitionPointPredicate(params.getInterval());
        Predicate<ChainInfo> blockDifficultyEqualToMaxTarget = new DifficultyEqualtoMaxTargetPredicate(params.getMaxTarget());
        Predicate<ChainInfo> genesisPredicate = new GenesisPredicate().negate();

        BlockChainRule newDifficultyAdjustmentAlgorithmRule = new NewDifficultyAdjustmentAlgorithmRule(
                difficultyAdjustmentActivatedPredicate
                        .and(genesisPredicate),
                params.getMaxTarget());

        BlockChainRule difficultyTransitionPointRule = new DifficultyTransitionPointRule(
                difficultyAdjustmentActivatedPredicate.negate()
                        .and(difficultyTransitionPointPredicate)
                        .and(genesisPredicate),
                params.getMaxTarget(),
                params.getInterval(),
                params.getTargetTimespan());

        BlockChainRule minimalDifficultyNoChangedRule = new MinimalDifficultyNoChangedRule(
                difficultyAdjustmentActivatedPredicate.negate()
                        .and(difficultyTransitionPointPredicate.negate())
                        .and(blockDifficultyEqualToMaxTarget)
                        .and(genesisPredicate),
                params.getMaxTarget());

        BlockChainRule emergencyDifficultyAdjustmentRule = new EmergencyDifficultyAdjustmentRule(
                difficultyAdjustmentActivatedPredicate.negate()
                        .and(difficultyTransitionPointPredicate.negate())
                        .and(blockDifficultyEqualToMaxTarget.negate())
                        .and(genesisPredicate),
                params.getMaxTarget());

        BlockChainRule minimumWorkRule = new MinimumWorkRule(params.getMaxTarget());

        BlockChainRule minimalDifficultyRule = new MinimalDifficultyRule(
                difficultyAdjustmentActivatedPredicate.and(genesisPredicate),
                params.getMaxTarget(),
                NetworkParameters.TARGET_SPACING);


        switch(params.getNet()) {
            case STN:
            case MAINNET:
                blockChainStoreRuleConfig = BlockChainStoreRuleConfig.builder()
                        .addRule(newDifficultyAdjustmentAlgorithmRule)
                        .addRule(difficultyTransitionPointRule)
                        .addRule(minimalDifficultyNoChangedRule)
                        .addRule(emergencyDifficultyAdjustmentRule)
                        .addRule(minimumWorkRule)
                        .build();
                break;


            case TESTNET3:
                BlockChainRule lastNonMinimalDifficultyRule = new LastNonMinimalDifficultyRule(
                        difficultyAdjustmentActivatedPredicate.negate()
                                .and(difficultyTransitionPointPredicate.negate())
                                .and(genesisPredicate),
                        params.getMaxTarget(),
                        params.getInterval(),
                        NetworkParameters.TARGET_SPACING);

                blockChainStoreRuleConfig = BlockChainStoreRuleConfig.builder()
                        .addRule(minimumWorkRule)
                        .addRule(minimalDifficultyRule)
                        .addRule(difficultyTransitionPointRule)
                        .addRule(lastNonMinimalDifficultyRule)
                        .build();
                break;

            default:
                blockChainStoreRuleConfig = BlockChainStoreRuleConfig.builder().build();

        }


        return blockChainStoreRuleConfig;
    }

}
