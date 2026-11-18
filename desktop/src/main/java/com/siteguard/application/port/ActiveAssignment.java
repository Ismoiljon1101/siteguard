package com.siteguard.application.port;

import com.siteguard.domain.Device;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.Worker;
import com.siteguard.domain.Zone;
import java.time.Instant;

/** An active assignment with its worker, device and current zone (zone fields null when unset). */
public record ActiveAssignment(HelmetAssignment assignment, Worker worker, Device device, Zone zone,
                               Instant zoneSetAt, String zoneSetBy) { }
