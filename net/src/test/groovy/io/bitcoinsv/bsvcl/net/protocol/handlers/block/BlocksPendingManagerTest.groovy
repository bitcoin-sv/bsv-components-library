package io.bitcoinsv.bsvcl.net.protocol.handlers.block

import com.google.common.base.Objects
import io.bitcoinsv.bsvcl.net.network.PeerAddress
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlocksPendingManager
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig
import spock.lang.Ignore
import spock.lang.Specification


/**
 * A Testing class to check that the Download CRITERIA and ACTIONS defined work well and the right Peers are choosen
 * for download Blocks:
 */
@Ignore
class BlocksPendingManagerTest extends Specification {

    // Test data set:

    PeerAddress peer1 = PeerAddress.localhost(0001);
    PeerAddress peer2 = PeerAddress.localhost(0002);
    PeerAddress peer3 = PeerAddress.localhost(0003);

    String blockA = "Block-A";
    String blockB = "Block-B";
    String blockC = "Block-C";
    List<String> pendingBlocks = Arrays.asList(blockA, blockB, blockC);

    /** A Tuple meaning this Peer is assigned to download this Block */
    class PeerBlockAssignment {
        PeerAddress peer;
        String block;

        PeerBlockAssignment(PeerAddress peer, String block) {
            this.peer = peer;
            this.block = block;
        }

        @Override
        int hashCode() { return Objects.hashCode(peer, block);}

        @Override
        boolean equals(Object obj) {
            if (obj == null) return false;
            PeerBlockAssignment other = (PeerBlockAssignment) obj
            return (this.peer.equals(other.peer) && this.block.equals(other.block));
        }
    }

    // It process all the available Peers and returns the Blocks assigned to them
    private List<PeerBlockAssignment> runAndGetAssignments(
            BlockDownloaderHandlerConfig.BestMatchCriteria bestMatchCriteria,
            BlockDownloaderHandlerConfig.BestMatchNotAvailableAction bestMatchNotAvailableAction,
            BlockDownloaderHandlerConfig.NoBestMatchAction noBestMatchAction,
            BlocksPendingManager blocksPendingManager,
            List<PeerAddress> availablePeers,
            List<PeerAddress> notAvailablePeers) {

        List<PeerBlockAssignment> result = new ArrayList<>()

        // We configure the BlocksPEndingManager:
        blocksPendingManager.add(pendingBlocks);
        blocksPendingManager.setBestMatchCriteria(bestMatchCriteria)
        blocksPendingManager.setBestMatchNotAvailableAction(bestMatchNotAvailableAction)
        blocksPendingManager.setNoBestMatchAction(noBestMatchAction)

        // Defensive Copy of the parameters:
        List<PeerAddress> availableP = new ArrayList<>(availablePeers)
        List<PeerAddress> notAvailableP = new ArrayList<>(notAvailablePeers)
        List<PeerAddress> peersToCheck = new ArrayList<>(availablePeers);

        for (PeerAddress currentPeer : peersToCheck) {

            List<PeerAddress> availablePeersWithoutCurrent = new ArrayList<PeerAddress>(availableP)
            availablePeersWithoutCurrent.remove(currentPeer);

            List<PeerAddress> notAvailablePeersWithoutCurrent = new ArrayList<PeerAddress>(notAvailableP)
            notAvailablePeersWithoutCurrent.remove(currentPeer);

            Optional<String> block = blocksPendingManager.extractMostSuitableBlockForDownload(
                    currentPeer,
                    availablePeersWithoutCurrent,
                    notAvailablePeersWithoutCurrent)

            if (block.isPresent()) {
                availableP.remove(currentPeer)
                notAvailableP.add(currentPeer);
                result.add(new PeerBlockAssignment(currentPeer, block.get()))
            }
        }
        return result;
    }

    /**
     * default criteria and Actions, all peers Available.
     * Blocks are assigned to Peers in the same order.
     * Expected result > P1:A, P2:B, P3:C
     */
    def "default criteria and Actions, all peers available. "() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1,blockA),
                new PeerBlockAssignment(peer2,blockB),
                new PeerBlockAssignment(peer3,blockC)
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE,  // not relevant
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,            // not relevant
                blocksPendingManager,
                Arrays.asList(peer1, peer2, peer3),     // Peers available
                new ArrayList<>())                      // Pers NOT available
        then:
        assignments.equals(expected)
    }

    /**
     * default criteria and Actions, 1 peers Available.
     * Expected result > P1:A
     */
    def "default criteria and Actions, 1 peers available. "() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(new PeerBlockAssignment(peer1, blockA))
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANYONE,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE,  // not relevant
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,            // not relevant
                blocksPendingManager,
                Arrays.asList(peer1),               // Peers available
                Arrays.asList(peer2, peer3))        // Peers NOR available
        then:
        assignments.equals(expected)
    }

    /**
     * BestMatchCriteria: FROM_ANNOUNCERS
     * BestMatchNotAvailableAction: DOWNLOAD_FROM_ANYONE
     * NoBestMatchAction:DOWNLOAD_FROM_ANYONE
     *
     * BlockA announced by Peer3
     * BlockB announced by Peer1
     * BlockC not announced by any Peer
     *
     * All Peers Available
     *
     * Expected result > P1:B, P2:C, P3:A
     */
    def "fromAnnouncers-test1"() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        blocksPendingManager.registerBlockAnnouncement(blockA, peer3)
        blocksPendingManager.registerBlockAnnouncement(blockB, peer1)

        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1, blockB),
                new PeerBlockAssignment(peer2, blockC),
                new PeerBlockAssignment(peer3, blockA),
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE,
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,
                blocksPendingManager,
                Arrays.asList(peer1, peer2, peer3),     // Peers available
                Arrays.asList())                        // Peers NOR available
        then:
        assignments.equals(expected)
    }

    /**
     * BestMatchCriteria: FROM_ANNOUNCERS
     * BestMatchNotAvailableAction: WAIT
     * NoBestMatchAction:DOWNLOAD_FROM_ANYONE
     *
     * BlockA announced by Peer3
     * BlockB announced by Peer1
     * BlockC not announced by any Peer
     *
     * Peer 3 NOT AVAILABLE
     *
     * Expected result > P1:B, P2:C
     * Block A is NOT assigned, since we WAIT for Peer 3 to become available in the future
     */
    def "fromAnnouncers-test2"() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        blocksPendingManager.registerBlockAnnouncement(blockA, peer3)
        blocksPendingManager.registerBlockAnnouncement(blockB, peer1)

        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1, blockB),
                new PeerBlockAssignment(peer2, blockC)
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.WAIT,
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,
                blocksPendingManager,
                Arrays.asList(peer1, peer2),    // Peers available
                Arrays.asList(peer3))           // Peers NOT available
        then:
        assignments.equals(expected)
    }

    /**
     * BestMatchCriteria: FROM_ANNOUNCERS
     * BestMatchNotAvailableAction: WAIT
     * NoBestMatchAction:WAIT
     *
     * BlockA announced by Peer3
     * BlockB announced by Peer1
     * BlockC not announced by any Peer
     *
     * Peer 3 MISSING
     *
     * Expected result > P1:B
     * - Block A should be downloaded by Peer3, but since the Peer is missing and we chose DOWNLOAD_FROM_ANYONE, it
     *   will be assigned to Peer1.
     * - Block B should be downloaded by Peer1, but Peer 1 has been assigned to Block A, and since we chose WAIT, then
     *   this block is NOT assigned.
     * - Block C has not been announced by anybody (so there is no Match), and since we chose WAIT, then
     *   this block is NOT assigned
     */
    def "fromAnnouncers-test3"() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        blocksPendingManager.registerBlockAnnouncement(blockA, peer3)
        blocksPendingManager.registerBlockAnnouncement(blockB, peer1)

        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1, blockB)
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.WAIT,
                BlockDownloaderHandlerConfig.NoBestMatchAction.WAIT,
                blocksPendingManager,
                Arrays.asList(peer1, peer2),    // Peers available
                Arrays.asList())                // Peers NOT available
        then:
        assignments.equals(expected)
    }

    /**
     * BestMatchCriteria: FROM_ANNOUNCERS
     * BestMatchNotAvailableAction: DOWNLOAD_FROM_ANYONE
     * NoBestMatchAction:DOWNLOAD_FROM_ANYONE
     *
     * BlockA announced by Peer3
     * BlockB announced by Peer1
     * BlockC not announced by any Peer
     *
     * Peer 3 MISSING
     *
     * Expected result > P1:A, P2:B
     * - Block A should be downloaded by Peer3, but since the Peer is missing and we chose DOWNLOAD_FROM_ANYONE, it
     *   will be assigned to Peer1.
     * - Block B should be downloaded by Peer1, but Peer 1 has been assigned to Block A, and since we chose
     *   DOWNLOAD_FROM_ANYONE, then it will be assigned to Peer2
     * - After this, all peers are busy so there are no more assignments
     */
    def "fromAnnouncers-test4"() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        blocksPendingManager.registerBlockAnnouncement(blockA, peer3)
        blocksPendingManager.registerBlockAnnouncement(blockB, peer1)

        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1, blockA),
                new PeerBlockAssignment(peer2, blockB)
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.DOWNLOAD_FROM_ANYONE,
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,
                blocksPendingManager,
                Arrays.asList(peer1, peer2),    // Peers available
                Arrays.asList())                // Peers NOT available
        then:
        assignments.equals(expected)
    }

    /**
     * BestMatchCriteria: FROM_ANNOUNCERS
     * BestMatchNotAvailableAction: WAIT
     * NoBestMatchAction:DOWNLOAD_FROM_ANYONE
     *
     * BlockA announced by Peer3
     * BlockB announced by Peer1
     * BlockC not announced by any Peer
     *
     * Peer 3 MISSING
     *
     * Expected result > P1:A, P2:C
     * - Block A should be downloaded by Peer3, but since the Peer is missing and we chose DOWNLOAD_FROM_ANYONE, it
     *   will be assigned to Peer1.
     * - Block B should be downloaded by Peer1, but Peer 1 has been assigned to Block A, and since we chose
     *   WAIT, then this Block will NOT be assigned
     * - block C does NOt have a Match, and since we chose DOWNLOAD_FROM_ANYONE and Peers 2 is still available, this
     *   block is assigned to Peer 2
     */
    def "fromAnnouncers-test5"() {
        given:
        BlocksPendingManager blocksPendingManager = new BlocksPendingManager();
        blocksPendingManager.registerBlockAnnouncement(blockA, peer3)
        blocksPendingManager.registerBlockAnnouncement(blockB, peer1)

        // Expected Assignment:
        List<PeerBlockAssignment> expected = Arrays.asList(
                new PeerBlockAssignment(peer1, blockA),
                new PeerBlockAssignment(peer2, blockC)
        )
        when:
        List<PeerBlockAssignment> assignments = runAndGetAssignments(
                BlockDownloaderHandlerConfig.BestMatchCriteria.FROM_ANNOUNCERS,
                BlockDownloaderHandlerConfig.BestMatchNotAvailableAction.WAIT,
                BlockDownloaderHandlerConfig.NoBestMatchAction.DOWNLOAD_FROM_ANYONE,
                blocksPendingManager,
                Arrays.asList(peer1, peer2),    // Peers available
                Arrays.asList())                // Peers NOT available
        then:
        assignments.equals(expected)
    }

}