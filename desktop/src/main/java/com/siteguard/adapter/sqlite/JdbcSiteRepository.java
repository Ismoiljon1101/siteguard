package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.SiteRepository;
import com.siteguard.domain.Site;
import com.siteguard.domain.Zone;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class JdbcSiteRepository implements SiteRepository {
    private final Connection c;

    JdbcSiteRepository(Connection c) { this.c = c; }

    private static final Jdbc.Mapper<Site> SITE = rs ->
            new Site(rs.getLong(1), rs.getString(2), Jdbc.instant(rs.getString(3)));
    private static final Jdbc.Mapper<Zone> ZONE = rs ->
            new Zone(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4));

    @Override public Site createSite(String name, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO sites(name, created_at) VALUES(?,?)", name, at);
        return new Site(id, name, at);
    }

    @Override public Optional<Site> findSite(long siteId) {
        return Jdbc.one(c, "SELECT site_id, name, created_at FROM sites WHERE site_id = ?", SITE, siteId);
    }

    @Override public Optional<Site> findSiteByName(String name) {
        return Jdbc.one(c, "SELECT site_id, name, created_at FROM sites WHERE name = ?", SITE, name);
    }

    @Override public List<Site> listSites() {
        return Jdbc.list(c, "SELECT site_id, name, created_at FROM sites ORDER BY name", SITE);
    }

    @Override public Zone createZone(long siteId, String name, int sortOrder) {
        long id = Jdbc.insert(c, "INSERT INTO zones(site_id, name, sort_order) VALUES(?,?,?)", siteId, name, sortOrder);
        return new Zone(id, siteId, name, sortOrder);
    }

    @Override public Optional<Zone> findZone(long zoneId) {
        return Jdbc.one(c, "SELECT zone_id, site_id, name, sort_order FROM zones WHERE zone_id = ?", ZONE, zoneId);
    }

    @Override public List<Zone> zones(long siteId) {
        return Jdbc.list(c, "SELECT zone_id, site_id, name, sort_order FROM zones WHERE site_id = ? ORDER BY sort_order, name", ZONE, siteId);
    }
}
