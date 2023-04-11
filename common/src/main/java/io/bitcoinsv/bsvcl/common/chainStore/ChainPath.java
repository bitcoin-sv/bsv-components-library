package io.bitcoinsv.bsvcl.common.chainStore;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author i.fernandez@nchain.com
 * @author d.vrankar@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class represents a Path in a blockchain, a straight line of Nodes from one origin to one destination. It
 * provides methods to search for a Node inside the Path or compare agains another to search for possible FORKS.
 *
 * (based on initial implementation by Domen Vrankar)
 */
public class ChainPath<NodeData> {

    private List<NodeData> path;
    private int startingHeight;

    /**
     * Constructor
     * @param nodes             List of Nodes of this Chain Path
     * @param startingHeight    Height that this Path represents of the whole blockchain is part of
     */
    public ChainPath(List<NodeData> nodes, int startingHeight) {
        this.path = nodes;
        this.startingHeight = startingHeight;
    }

    /** Convenience constructor */
    public ChainPath(int startingHeight) {
        this(new ArrayList<>(), startingHeight);
    }


    /**
     * Returns whether the given Node is in this Chain Path. A Node is considered to be IN this Chain Path if
     * its one of the Nodes of this Path, or tis height is BELOW this path, in this case even though the Node
     * is NOT stored here, it can be considered part of the "theorical" Chain Path that begins from genesis and
     * continues on this Chain Path.
     *
     * @param nodeId  Node to search
     * @param height  Height of the Node
     * @return
     *  - TRUE: Block is in this Chain Path, or its below it
     *  - FALSE: Block is NOT in the chain
     *  - null: block is beyond this chain Path (height is higher than this whole Chain Path)
     */
    public Boolean isInChain(NodeData nodeId, int height ) {
        assert (height >= 0) : "Index may never be less than 0.";

        if (height < startingHeight) {
            return true;
        } else if (height >= path.size() + startingHeight) {
            return (path.isEmpty() ? false : null);
        }
        int offset = height - startingHeight;
        return nodeId.equals(path.get(offset) );
    }

    /**
     * Returns the index of the Node in this ChainPath that represents a FORK with the ChainPath given. If the
     * result is N, then the Node at height N is present in both ChainPaths, but under it both ChainPaths contain
     * different nodes (different children of the same parent)
     *
     * @param other ChainPath to compare against
     * @return
     *   - N : index of the Node in this Path that is a Fork with the other
     *   - -1: There is no fork
     *   - null: can't answer as the blocks are pruned too far back
     */
    public Integer getForkPoint(ChainPath<NodeData> other) {

        // First Node in common is the Height after wish they overlap:
        // Offsets are the indexes where both paths can start comparing nodes to each other:
        int firstCommonKnown    = Math.max(startingHeight, other.startingHeight);
        int offset              = firstCommonKnown - startingHeight;
        int offsetOther         = firstCommonKnown - other.startingHeight;

        if (other.path.size() == 0 || path.size() == 0)                         { return -1; }
        if ((firstCommonKnown == 0) && (!path.get(0).equals(other.path.get(0)))){ return -1;}

        if (path.size() < firstCommonKnown - startingHeight)                    { return null; }
        if (other.path.size() < firstCommonKnown - other.startingHeight)        { return null; }
        if (!path.get(offset).equals(other.path.get(offsetOther)))              { return null; }

        if (!path.get(offset).equals(other.path.get(offsetOther)))              { return -1; }


        // We get the FIRST Node in both chains that is DIFFERENT, and the Fork would be the Node BEFORE that
        // (the parent). We do a BINARY Search:

        int beginCheck = offset;
        int endCheck = Math.min(other.path.size() - offsetOther, path.size() - offset) - 1 + offset;

        while (beginCheck < endCheck) {
            int indexToCheck = ((endCheck - beginCheck + 1) / 2) + beginCheck;
            int indexOtherToCheck = (offset == 0)? indexToCheck + offsetOther : indexToCheck - offset;
            if (other.path.get(indexOtherToCheck).equals(path.get(indexToCheck))) {
                beginCheck = indexToCheck;
            } else {
                endCheck = indexToCheck - 1;
            }
        }
        return beginCheck + startingHeight;
    }

    /**
     * Returns the Block Id identified by the height given, if it's stored in this Chain Path.
     */
    public Optional<NodeData> getNode(int height ) {
        assert (height >= 0) : "Index may never be less than 0.";

        if (height < startingHeight) {
            return Optional.empty();
        }
        int offset = height - startingHeight;
        return (offset < path.size() ? Optional.of(path.get(offset)) : Optional.empty());
    }

    /** Returns the size (length) of this ChainPath */
    public int size() { return path.size();}

    /**
     * Returns a list with the Nodes of this Chain Path, starting at the height given
     */
    public List<NodeData> getNodesFrom(int startAtHeight) {
        assert (startAtHeight >= 0) : "Index may never be less than 0.";

        if (startAtHeight < startingHeight) {
            throw new RuntimeException( "getBlocksFrom constraint broken: startAtHeight < prunedBeforeHeight" );
        }
        int offset = startAtHeight - startingHeight;
        if (offset >= path.size()) {
            return null;
        }
        return path.subList(offset, path.size());
    }

    /**
     * Returns a list with the Nodes of this Chain Path, starting at the height given
     */
    public List<NodeData> getNodes() {
        return path;
    }

    /**
     * Returns the starting height (height of the first Block) of this Path
     */
    public int getStartingHeight() { return startingHeight;}

    @Override
    public boolean equals(Object other) {
        if (other instanceof ChainPath) {
            ChainPath<NodeData> otherChain = (ChainPath<NodeData>) other;
            return this.startingHeight == otherChain.startingHeight
                    && path.size() == otherChain.path.size()
                    && IntStream.range(0, path.size())
                    .allMatch(i -> path.get(i).equals(otherChain.path.get(i)));
        }
        return false;
    }

}
