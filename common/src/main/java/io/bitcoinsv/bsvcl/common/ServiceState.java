package io.bitcoinsv.bsvcl.common;

/** common states for a service */
public enum ServiceState {
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
    PAUSED,
}

