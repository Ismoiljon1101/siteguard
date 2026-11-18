package com.siteguard.application.port;

import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository {
    /** Insert once per source identity. False when it already exists. */
    boolean insertIfAbsent(Incident incident);
    Optional<Incident> find(String sourceIdentity);
    Optional<IncidentView> view(String sourceIdentity);
    boolean hasUnresolved(String deviceId, IncidentType type);
    /** Conditional state change. False when the current state is not from. */
    boolean transition(String sourceIdentity, IncidentState from, IncidentState to);
    /** Append a history row. */
    long appendAction(IncidentAction action);
    List<IncidentAction> actions(String sourceIdentity);
    List<IncidentView> search(IncidentFilter filter);
    List<Incident> openSos();
    void setEscalation(String sourceIdentity, EscalationLevel level);
}
