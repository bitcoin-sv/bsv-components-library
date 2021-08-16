/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.tools.util;

import io.bitcoinj.bitcoin.api.base.Header;
import io.bitcoinj.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;
import io.bitcoinj.exception.VerificationException;

import java.math.BigInteger;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 * @date 24/02/2021
 */
public class PowUtil {

    public static boolean hasEqualDifficulty(HeaderReadOnly firstBlock, HeaderReadOnly secondBlock) {
        return firstBlock.getDifficultyTarget() == secondBlock.getDifficultyTarget();
    }

    public static boolean hasEqualDifficulty(long a, BigInteger b) {
        return a == Utils.encodeCompactBits(b);
    }

    /**
     * Returns true if the difficulty has been succesfully verified
     */
    public static boolean verifyDifficulty(BigInteger maxTarget, BigInteger expectedTarget, long candidateTargetCompact) {
        if (expectedTarget.compareTo(maxTarget) > 0) {
            expectedTarget = maxTarget;
        }

        int accuracyBytes = (int) (candidateTargetCompact >>> 24) - 3;
        long receivedTargetCompact = candidateTargetCompact;

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        expectedTarget = expectedTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(expectedTarget);

        if (newTargetCompact != receivedTargetCompact) {
           return false;
        }

        return true;
    }

    public static boolean verifyProofOfWork(HeaderReadOnly blockHeader, BigInteger maxTarget) {
        BigInteger target = blockHeader.getDifficultyTargetAsInteger();
        if (target.signum() <= 0 || target.compareTo(maxTarget) > 0) {
                return false;
        }

        BigInteger hash_ = blockHeader.getHash().toBigInteger();
        if (hash_.compareTo(target) > 0) {
            // Proof of work check failed!
            return false;
        }

        return true;
    }
}
