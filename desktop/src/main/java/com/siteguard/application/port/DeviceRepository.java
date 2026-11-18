package com.siteguard.application.port;

import com.siteguard.domain.Device;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository {
    Device register(String deviceId, long siteId, String displayName, boolean simulated, Instant at);
    Optional<Device> find(String deviceId);
    List<Device> list(long siteId);
    List<Device> listActive();
    void retire(String deviceId);
}
