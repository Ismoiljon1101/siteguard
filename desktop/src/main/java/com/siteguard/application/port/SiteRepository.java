package com.siteguard.application.port;

import com.siteguard.domain.Site;
import com.siteguard.domain.Zone;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SiteRepository {
    Site createSite(String name, Instant at);
    Optional<Site> findSite(long siteId);
    Optional<Site> findSiteByName(String name);
    List<Site> listSites();
    Zone createZone(long siteId, String name, int sortOrder);
    Optional<Zone> findZone(long zoneId);
    List<Zone> zones(long siteId);
}
