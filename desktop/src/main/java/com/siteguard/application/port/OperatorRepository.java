package com.siteguard.application.port;

import com.siteguard.domain.Operator;
import com.siteguard.domain.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OperatorRepository {
    Operator create(String loginId, String displayName, Role role, Instant at);
    Optional<Operator> find(long operatorId);
    Optional<Operator> findByLogin(String loginId);
    List<Operator> list();
    long count();
    void grantSite(long operatorId, long siteId);
    Set<Long> siteIds(long operatorId);
}
