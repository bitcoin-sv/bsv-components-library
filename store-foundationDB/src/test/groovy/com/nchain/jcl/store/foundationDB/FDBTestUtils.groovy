package com.nchain.jcl.store.foundationDB

class FDBTestUtils {

    // If true, we use Docker image, false: we use local FDB installation
    public static boolean useDocker = false;

    // Location of the Scrit Files to Start/Stop the Foundation DB running in Docker:
    private static final START_SCRIPT = "./src/test/resources/startFDBForTesting.sh"
    private static final STOP_SCRIPT = "./src/test/resources/stopFDBForTesting.sh"

    static void checkFDBBefore() {
        if (useDocker) {
            println("Starting FoundationDB in a Docker container...")
            def sout = new StringBuilder(), serr = new StringBuilder()
            def script = START_SCRIPT.execute()
            script.consumeProcessOutput(sout, serr)
            script.waitFor()
            println(sout)
        } else {
            println("Using FDB Local installation")
        }

    }

    static void checkFDBAfter() {
        if (useDocker) {
            println("Stopping FoundationDB in a Docker container...")
            def sout = new StringBuilder(), serr = new StringBuilder()
            def script = STOP_SCRIPT.execute()
            script.consumeProcessOutput(sout, serr)
            script.waitFor()
            println(sout)
        } else {
            println("FDB Local installation has been used for this test.")
        }
    }
}
