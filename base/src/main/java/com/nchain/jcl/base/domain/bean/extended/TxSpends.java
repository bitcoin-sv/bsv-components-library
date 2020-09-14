package com.nchain.jcl.base.domain.bean.extended;


import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.*;

/** <p>The spends of a transactions outputs.</p>
 *
 * <p>A transaction output can be "spent" by multiple transactions but only one of those transactions can be confirmed
 * in a given path of blocks. Which is to say that there can be multiple transactions spending a transaction output but
 * only one can be confirmed (double-spend scenario), or two or more transactions can spend the same output if these transactions are
 * confirmed in different forks of the blockchain.</p>
 *
 * <p>This structure is an expandable list where the index in the list represents the index of the output being spent. It
 * is expandable because at the time of registering the spend the system may not know the total number of outputs of the
 * transaction being spent.</p>
 */
public class TxSpends {
    private List<Set<Sha256Wrapper>> spends;

    public TxSpends(int numOutputs) {
        this.spends = new ArrayList<>(numOutputs);
        for(int i = 0; i < numOutputs; i++ ) {
            spends.add(new HashSet<>());
        }
    }

    public TxSpends(int index, Sha256Wrapper spendingTx) {
        this(index);
        spends.add(index, new HashSet<>(Collections.singleton(spendingTx)));
    }

    public void addSpendingTx(int index, Sha256Wrapper spendingTx) {
        if (spends == null) {
            spends = new ArrayList<>();
        }
        if (index >= spends.size()) {
            for (int i = spends.size(); i < index; i++) {
                spends.add(new HashSet<>());
            }
            spends.add(index, new HashSet<>(Collections.singleton(spendingTx)));
        } else {
            Set<Sha256Wrapper> val = spends.get(index);
            if (val == null) {
                spends.add(index, new HashSet<>(Collections.singleton(spendingTx)));
            } else {
                val.add(spendingTx);
                spends.set(index, val);
            }
        }
    }

    public Set<Sha256Wrapper> getSpends(int index) {
        if (spends == null || index >= spends.size()) {
            return Collections.emptySet();
        }
        return spends.get(index);
    }

    public int numOutputs() {
        if (spends == null) return 0;
        return spends.size();
    }

    @Override
    public String toString() {
        return "TransactionSpends{" +
                "txSpends=" + spends +
                '}';
    }
}
