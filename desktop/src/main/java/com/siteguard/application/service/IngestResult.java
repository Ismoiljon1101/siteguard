package com.siteguard.application.service;

/** Outcome of one ingest call. incidentIdentity is the incident this message opened, or the existing one for a duplicate event. */
public record IngestResult(Status status, String incidentIdentity) {
    public enum Status {
        /** Stored as the device current reading, or the event and its incident committed. */
        STORED,
        /** Valid and stored for history, but older than the current reading: rules and current values untouched. */
        STORED_OUT_OF_ORDER,
        /** Same device/boot/seq already committed. Nothing changed. */
        DUPLICATE,
        /** Valid, but another reading was stored for this device in the same wall-clock second (max 1 Hz). */
        RATE_LIMITED,
        /** Device not registered or retired. */
        UNKNOWN_DEVICE,
        /** Simulated message for a live device or the reverse. Rejected so a simulation never marks a helmet live. */
        SOURCE_MISMATCH
    }

    /** True when the durable receipt may be sent (protocol): stored now or already committed earlier. */
    public boolean receiptStored() {
        return status == Status.STORED || status == Status.STORED_OUT_OF_ORDER || status == Status.DUPLICATE;
    }
}
