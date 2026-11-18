package com.siteguard.domain;

/** Operator identity. Credentials are stored separately and never leave the adapter. */
public record Operator(long operatorId, String loginId, String displayName, Role role) { }
