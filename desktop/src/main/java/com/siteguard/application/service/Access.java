package com.siteguard.application.service;

import com.siteguard.application.port.Repositories;
import com.siteguard.domain.Operator;

/** Operator lookup and site scoping. A safety director sees all sites; others only granted sites. */
final class Access {
    private Access() { }

    static Operator operator(Repositories r, long operatorId) {
        return r.operators().find(operatorId).orElseThrow(() -> new ValidationException("Unknown operator"));
    }

    static void requireSite(Repositories r, Operator operator, long siteId) {
        if (operator.role().seesAllSites()) return;
        if (!r.operators().siteIds(operator.operatorId()).contains(siteId)) {
            throw new AuthorizationException("Operator has no access to this site");
        }
    }

    static Operator operatorForSite(Repositories r, long operatorId, long siteId) {
        Operator op = operator(r, operatorId);
        requireSite(r, op, siteId);
        return op;
    }
}
