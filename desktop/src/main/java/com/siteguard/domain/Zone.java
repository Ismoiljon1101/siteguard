package com.siteguard.domain;

/** A named area chosen by an operator. Never live location. */
public record Zone(long zoneId, long siteId, String name, int sortOrder) { }
