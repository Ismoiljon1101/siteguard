package com.siteguard.ui;

public record IncidentRow(String id, String type, String severity, String state,
        String detail, String occurredAt) { }
