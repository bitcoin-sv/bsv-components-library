package com.nchain.jcl.store.foundationDB

class DockerTestUtils {

    // Location of the SCrit Files to Start/Stop the Foundation DB running in Docker:
    private static final START_SCRIPT = "./src/test/resources/startFDBForTesting.sh"
    private static final STOP_SCRIPT = "./src/test/resources/stopFDBForTesting.sh"

    static void startDocker() {
        println("Starting FoundationDB in a Docker container...")
        def sout = new StringBuilder(), serr = new StringBuilder()
        def script = START_SCRIPT.execute()
        script.consumeProcessOutput(sout, serr)
        script.waitFor()
        println(sout)
    }

    static void stopDocker() {
        println("Stopping FoundationDB in a Docker container...")
        def sout = new StringBuilder(), serr = new StringBuilder()
        def script = STOP_SCRIPT.execute()
        script.consumeProcessOutput(sout, serr)
        script.waitFor()
        println(sout)
    }
}
