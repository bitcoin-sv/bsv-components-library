package io.bitcoinsv.bsvcl.common;

/** common states for a service */
public enum ServiceState {
    /** service is created */
    CREATED,
    /** service is starting */
    STARTING,
    /** service is running */
    RUNNING,
    /** service is stopping */
    STOPPING,
    /** service is stopped */
    STOPPED,
    /** service is in an error state */
    ERROR,
    /** service is paused */
    PAUSED;

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isStopped() {
        return this == STOPPED;
    }
}

