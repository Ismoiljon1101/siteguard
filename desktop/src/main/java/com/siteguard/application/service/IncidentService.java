package com.siteguard.application.service;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.application.port.Repositories;
import com.siteguard.application.port.Store;
import com.siteguard.domain.ActionType;
import com.siteguard.domain.Device;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Operator;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.domain.StoredReading;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Incident workflow: OPEN, ACKNOWLEDGED, RESOLVED. No skipping, no reopening. Every step is appended to
 * incident_actions with the operator. Acknowledging never silences a helmet alarm. Blocking.
 */
public final class IncidentService {
    /** Stored readings shown before and after an incident (playback). */
    public static final Duration PLAYBACK_WINDOW = Duration.ofMinutes(5);

    private final Store store;
    private final Clock clock;

    public IncidentService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    /** One list row: the incident with its recorded worker and zone, plus time unacknowledged (OPEN only). */
    public record IncidentRow(IncidentView view, Duration unacknowledgedFor) { }

    /** Everything the incident detail screen needs. */
    public record IncidentDetail(IncidentView view, List<IncidentAction> actions, List<StoredReading> playback,
                                 Duration unacknowledgedFor, boolean environmentConditionCleared) { }

    public IncidentView acknowledge(String sourceIdentity, long operatorId) {
        return store.inTransaction(r -> {
            Operator op = scoped(r, sourceIdentity, operatorId);
            move(r, sourceIdentity, IncidentState.ACKNOWLEDGED);
            append(r, sourceIdentity, ActionType.ACKNOWLEDGED, op, null, null);
            return r.incidents().view(sourceIdentity).orElseThrow();
        });
    }

    /** Notes are allowed in any state and never change it. */
    public IncidentView addNote(String sourceIdentity, long operatorId, String note) {
        String text = requireText(note, "A note is required");
        return store.inTransaction(r -> {
            Operator op = scoped(r, sourceIdentity, operatorId);
            append(r, sourceIdentity, ActionType.NOTE, op, text, null);
            return r.incidents().view(sourceIdentity).orElseThrow();
        });
    }

    /** Needs an ACKNOWLEDGED incident, a category and a non-blank note. */
    public IncidentView resolve(String sourceIdentity, long operatorId, ResolutionCategory category, String note) {
        if (category == null) throw new ValidationException("A resolution category is required");
        String text = requireText(note, "A resolution note is required");
        return store.inTransaction(r -> {
            Operator op = scoped(r, sourceIdentity, operatorId);
            move(r, sourceIdentity, IncidentState.RESOLVED);
            append(r, sourceIdentity, ActionType.RESOLVED, op, text, category);
            return r.incidents().view(sourceIdentity).orElseThrow();
        });
    }

    public Optional<IncidentDetail> detail(long operatorId, String sourceIdentity) {
        Instant now = clock.instant();
        return store.read(r -> {
            Optional<IncidentView> view = r.incidents().view(sourceIdentity);
            if (view.isEmpty()) return Optional.empty();
            Access.operatorForSite(r, operatorId, view.get().siteId());
            Incident i = view.get().incident();
            List<StoredReading> playback = r.readings().range(i.deviceId(),
                    i.occurredAt().minus(PLAYBACK_WINDOW), i.occurredAt().plus(PLAYBACK_WINDOW));
            boolean cleared = false;
            if (i.type() == IncidentType.ENVIRONMENT && i.state() != IncidentState.RESOLVED) {
                double limit = r.config().current().tempWarnC();
                cleared = r.readings().latest(i.deviceId())
                        .map(x -> x.temperatureC() != null && x.temperatureC() < limit).orElse(false);
            }
            return Optional.of(new IncidentDetail(view.get(), r.incidents().actions(sourceIdentity), playback,
                    unack(i, now), cleared));
        });
    }

    /** Newest first. A site-limited operator must name a site. */
    public List<IncidentRow> list(long operatorId, IncidentFilter filter) {
        Instant now = clock.instant();
        return store.read(r -> {
            Operator op = Access.operator(r, operatorId);
            if (filter.siteId() != null) Access.requireSite(r, op, filter.siteId());
            else if (!op.role().seesAllSites()) throw new ValidationException("A site is required");
            return r.incidents().search(filter).stream().map(v -> new IncidentRow(v, unack(v.incident(), now))).toList();
        });
    }

    /**
     * Raise escalation on unacknowledged SOS incidents: level 1 at 30 s, level 2 at 120 s (D20, PROPOSED placeholders).
     * Never changes state. Call about once a second. Returns how many incidents rose a level.
     */
    public int escalateOverdue() {
        Instant now = clock.instant();
        return store.inTransaction(r -> {
            int changed = 0;
            for (Incident i : r.incidents().openSos()) {
                EscalationLevel target = EscalationLevel.forUnacknowledgedAge(Duration.between(i.occurredAt(), now));
                if (target.level() > i.escalation().level()) {
                    r.incidents().setEscalation(i.sourceIdentity(), target);
                    changed++;
                }
            }
            return changed;
        });
    }

    private static Duration unack(Incident i, Instant now) {
        return i.state() == IncidentState.OPEN ? Duration.between(i.occurredAt(), now) : null;
    }

    private static String requireText(String text, String message) {
        if (text == null || text.isBlank()) throw new ValidationException(message);
        return text.strip();
    }

    private static Operator scoped(Repositories r, String identity, long operatorId) {
        Incident incident = r.incidents().find(identity).orElseThrow(() -> new NotFoundException("Unknown incident"));
        Device device = r.devices().find(incident.deviceId()).orElseThrow(() -> new NotFoundException("Unknown device"));
        return Access.operatorForSite(r, operatorId, device.siteId());
    }

    private void move(Repositories r, String identity, IncidentState to) {
        Incident incident = r.incidents().find(identity).orElseThrow(() -> new NotFoundException("Unknown incident"));
        if (!incident.state().canMoveTo(to)) {
            throw new InvalidTransitionException("Cannot move an incident from " + incident.state() + " to " + to);
        }
        if (!r.incidents().transition(identity, incident.state(), to)) {
            throw new InvalidTransitionException("The incident changed state; reload and retry");
        }
    }

    private void append(Repositories r, String identity, ActionType type, Operator op, String note, ResolutionCategory category) {
        r.incidents().appendAction(new IncidentAction(0, identity, type, op.displayName(), op.operatorId(), note, category, clock.instant()));
    }
}
