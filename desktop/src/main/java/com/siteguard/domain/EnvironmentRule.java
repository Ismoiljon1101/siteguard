package com.siteguard.domain;

import java.util.List;

/** Environment rule: temperature at or above the limit for N consecutive samples. */
public final class EnvironmentRule {
    private EnvironmentRule() { }

    /** newestFirst: most recent samples, newest first; null entries are sensor faults and break a run. */
    public static boolean violated(ConfigVersion config, List<Double> newestFirst) {
        int n = config.tempConsecutiveSamples();
        if (newestFirst.size() < n) return false;
        for (int i = 0; i < n; i++) {
            Double t = newestFirst.get(i);
            if (t == null || t < config.tempWarnC()) return false;
        }
        return true;
    }
}
