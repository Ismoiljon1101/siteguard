package com.siteguard.adapter.sqlite;

/** An append-only operator action, ordered by insertion in repository queries. */
public record IncidentAction(String sourceIdentity, String type, String operator,
                             String note, String recordedAt) { }
