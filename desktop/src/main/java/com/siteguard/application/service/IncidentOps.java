package com.siteguard.application.service;

import com.siteguard.application.port.Repositories;
import com.siteguard.domain.ActionType;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentAction;
import java.time.Instant;

/** Shared incident creation. Duplicate-protected by source identity; writes the OPENED history row. */
final class IncidentOps {
    private IncidentOps() { }

    static boolean open(Repositories r, Incident incident, Instant now) {
        if (!r.incidents().insertIfAbsent(incident)) return false;
        r.incidents().appendAction(new IncidentAction(0, incident.sourceIdentity(), ActionType.OPENED, "system", null,
                null, null, now));
        return true;
    }
}
