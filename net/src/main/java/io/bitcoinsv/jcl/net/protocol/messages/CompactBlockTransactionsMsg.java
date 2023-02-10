package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.Tx;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This message is received as a response to "getcmpctblck" message.
 * It holds a chunk of compact transactions data for the requested block.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "block_hash" (32 bytes) char[]
 *    Hash of a block this chunk is part of
 *
 *  - field: "start_tx_index" (1+ bytes) var_int
 *    Transaction's block Index of the first transaction in this chunk
 *
 *  - field: "coinbase" (0+ bytes) transaction
 *    If start_tx_index is 0, it contains a coinbase transaction
 *
 *  - field: "count" (1+ bytes) var_int
 *    Number of compact transactions in this chunk (max: MAXIMUM_NUMBER_OF_TRANSACTIONS)
 *
 *  - field: "compact_transactions" (33+ bytes) compact_transaction[]
 *    Array of compact transactions.
 *
 */
public final class CompactBlockTransactionsMsg extends BodyMessage implements Serializable {
    public static final int MAXIMUM_NUMBER_OF_TRANSACTIONS = 6000;
    public static final String MESSAGE_TYPE = "cmpctblcktxs";

    private final HashMsg blockHash;
    private final VarIntMsg startTxIndex;
    private final Optional<TxMsg> coinbaseTransaction;
    private final VarIntMsg numberOfTransactions;
    private final List<CompactTransactionMsg> compactTransactions;

    public CompactBlockTransactionsMsg(HashMsg blockHash, VarIntMsg startTxIndex, Optional<TxMsg> coinbaseTransaction,
                                       List<CompactTransactionMsg> compactTransactions,
                                       byte[] extraBytes, long checksum) {
        super(extraBytes, checksum);
        this.blockHash = blockHash;
        this.startTxIndex = startTxIndex;
        this.coinbaseTransaction = coinbaseTransaction;
        numberOfTransactions = VarIntMsg.builder().value(compactTransactions.size()).build();
        this.compactTransactions = compactTransactions.stream().collect(Collectors.toUnmodifiableList());
        init();
    }

    @Override
    protected long calculateLength() {
        long length = blockHash.calculateLength() + startTxIndex.getLengthInBytes() + numberOfTransactions.getLengthInBytes() +
                compactTransactions.stream().mapToLong(h -> h.getLengthInBytes()).sum() + 1;
        if (coinbaseTransaction.isPresent()) {
            length += coinbaseTransaction.get().getLengthInBytes();
        }
        return length;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(numberOfTransactions.getValue() <= MAXIMUM_NUMBER_OF_TRANSACTIONS,
                "Compact block transactions message exceeds maximum size");
        Preconditions.checkArgument(numberOfTransactions.getValue() ==  compactTransactions.size(),
                "Compact block transactions list size and count value are not the same.");
        if (startTxIndex.getValue() == 0) {
            Preconditions.checkArgument(coinbaseTransaction.isPresent(),
                    "This compact block transactions message should contain a coinbase transaction");
        } else {
            Preconditions.checkArgument(coinbaseTransaction.isEmpty(),
                    "This compact block transactions message should not contain a coinbase transaction");
        }
    }

    @Override
    public String getMessageType() { return MESSAGE_TYPE; }
    public HashMsg getBlockHash() { return blockHash; }
    public VarIntMsg getStartTxIndex() { return startTxIndex; }
    public Optional<TxMsg> getCoinbaseTransaction() { return coinbaseTransaction; }
    public VarIntMsg getNumberOfTransactions() { return numberOfTransactions; }
    public List<CompactTransactionMsg> getCompactTransactions() { return compactTransactions; }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), blockHash, startTxIndex, coinbaseTransaction.get(), numberOfTransactions, compactTransactions);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false; }
        CompactBlockTransactionsMsg other = (CompactBlockTransactionsMsg) obj;
        return Objects.equal(startTxIndex, other.startTxIndex) &&
                Objects.equal(numberOfTransactions, other.numberOfTransactions) &&
                Objects.equal(blockHash, other.blockHash) &&
                Objects.equal(coinbaseTransaction.get(), other.coinbaseTransaction.get()) &&
                Objects.equal(compactTransactions, other.compactTransactions);
    }

    @Override
    public String toString() {
        return "CompactBlockTransactionsMsg(block_hash=" + blockHash + ", start_tx_index=" + startTxIndex + ", count=" +
                numberOfTransactions + ", compact_transactions=" + compactTransactions + ")";
    }

    public static CompactBlockTransactionsMsgBuilder builder() {
        return new CompactBlockTransactionsMsgBuilder();
    }

    @Override
    public CompactBlockTransactionsMsgBuilder toBuilder() {
        return new CompactBlockTransactionsMsgBuilder(super.extraBytes, super.checksum)
                .blockHash(blockHash).startTxIndex(startTxIndex).coinbaseTransaction(coinbaseTransaction).compactTransactions(compactTransactions);
    }

    /**
     * Builder
     */
    public static class CompactBlockTransactionsMsgBuilder extends BodyMessageBuilder {
        private HashMsg blockHash;
        private VarIntMsg startTxIndex;
        private Optional<TxMsg> coinbaseTransaction = Optional.empty();
        private List<CompactTransactionMsg> compactTransactions;

        public CompactBlockTransactionsMsgBuilder() {}
        public CompactBlockTransactionsMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}


        public CompactBlockTransactionsMsgBuilder blockHash(HashMsg blockHash) {
            this.blockHash = blockHash;
            return this;
        }

        public CompactBlockTransactionsMsgBuilder blockHash(Sha256Hash blockHash) {
            this.blockHash = HashMsg.builder().hash(blockHash.getBytes()).build();
            return this;
        }

        public CompactBlockTransactionsMsgBuilder startTxIndex(int startTxIndex) {
            this.startTxIndex = VarIntMsg.builder().value(startTxIndex).build();
            return this;
        }

        public CompactBlockTransactionsMsgBuilder startTxIndex(VarIntMsg startTxIndex) {
            this.startTxIndex = startTxIndex;
            return this;
        }

        public CompactBlockTransactionsMsgBuilder coinbaseTransaction(Optional<TxMsg> coinbaseTransaction) {
            this.coinbaseTransaction = coinbaseTransaction;
            return this;
        }

        public CompactBlockTransactionsMsgBuilder coinbaseTxBean(Optional<Tx> coinbaseTransaction) {
            if (coinbaseTransaction.isPresent()) {
                this.coinbaseTransaction = Optional.of(TxMsg.fromBean(coinbaseTransaction.get()));
            }
            return this;
        }

        public CompactBlockTransactionsMsgBuilder compactTransactions(List<CompactTransactionMsg> compactTransactions) {
            this.compactTransactions = compactTransactions;
            return this;
        }

        public CompactBlockTransactionsMsg build() {
            return new CompactBlockTransactionsMsg(blockHash, startTxIndex, coinbaseTransaction, compactTransactions, extraBytes, checksum);
        }
    }
}