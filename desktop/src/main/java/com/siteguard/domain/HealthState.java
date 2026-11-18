package com.siteguard.domain;

/** Device health shown to operators. Health never creates or resolves an incident. */
public enum HealthState { ONLINE, STALE, OFFLINE, SENSOR_FAULT, NO_DATA, UNASSIGNED }
