package com.siteguard.adapter.simulation;

/** Per-helmet scripted behaviour of the fleet simulator. All output is labelled simulated. */
public enum HelmetScenario {
    NORMAL,
    /** Air temperature at 36 C or above on every sample. */
    ENVIRONMENT_WARNING,
    /** Normal readings, then one SOS event on the 6th second of the scenario. */
    SOS,
    /** Normal readings, then one MOTION event on the 9th second of the scenario. */
    MOTION,
    /** Environment sensor reports fault: null temperature and humidity. */
    SENSOR_FAULT,
    /** Three normal seconds, then silence (goes STALE after 5 s, OFFLINE after 30 s). */
    STOPPED
}
