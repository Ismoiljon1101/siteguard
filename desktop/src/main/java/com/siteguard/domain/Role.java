package com.siteguard.domain;

public enum Role {
    SAFETY_DIRECTOR, SITE_MANAGER, SUPERVISOR;

    /** A safety director sees every site; other roles see only sites granted to them. */
    public boolean seesAllSites() { return this == SAFETY_DIRECTOR; }
}
