package io.bitcoinsv.jcl.store.blockChainStore.validation.rules;

import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.tools.util.PowUtil;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;


import java.math.BigInteger;

/**
 *
 */
public class MinimumWorkRule extends AbstractBlockChainRule {

    private final BigInteger maxTarget;

    public MinimumWorkRule(BigInteger maxTarget) {
        super();
        this.maxTarget = maxTarget;
    }

    @Override
    public void checkRule(ChainInfo candidateBlock, BlockChainStore blockStore) throws BlockChainRuleFailureException {
        if(!PowUtil.verifyProofOfWork(candidateBlock.getHeader(), maxTarget)){
            throw new BlockChainRuleFailureException("block does not have enough proof of work");
        }
    }

}
