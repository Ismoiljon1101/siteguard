package com.siteguard.application.port;

import com.siteguard.domain.Incident;

/** Incident joined with its recorded assignment: worker, helmet label, zone. Null worker and zone for unassigned helmets. */
public record IncidentView(Incident incident, long siteId, String deviceLabel, Long workerId,
                           String workerName, String zoneName) { }
