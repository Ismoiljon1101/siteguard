package com.siteguard.domain;

/** Synthetic or measured acceleration in metres per second squared. */
public record Acceleration(double x, double y, double z) {
    public Acceleration {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Acceleration must be finite");
        }
    }
}
