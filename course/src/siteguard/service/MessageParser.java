package siteguard.service;

import siteguard.exceptions.InvalidMessageException;
import siteguard.model.HelmetEvent;
import siteguard.model.Message;
import siteguard.model.Reading;

/**
 * Turns one text line into a Reading or a HelmetEvent.
 *
 * Line format (one message per line, fields separated by commas):
 *   T,deviceId,bootId,seq,uptimeMs,sim,temperatureC,humidityPct,ax,ay,az,envStatus,motionStatus
 *   E,deviceId,bootId,seq,uptimeMs,sim,SOS|MOTION
 * sim is 1 for simulated data and 0 for real hardware.
 * When a sensor is faulty its status is "fault" and its values are written as NA.
 */
public class MessageParser {

    public static final int MAX_LINE_LENGTH = 256;

    public Message parse(String line) throws InvalidMessageException {
        if (line == null || line.trim().isEmpty()) {
            throw new InvalidMessageException("Empty message");
        }
        line = line.trim();
        if (line.length() > MAX_LINE_LENGTH) {
            throw new InvalidMessageException("Message is too long");
        }
        String[] parts = line.split(",");
        String kind = parts[0];

        if (kind.equals("T")) {
            return parseReading(parts);
        } else if (kind.equals("E")) {
            return parseEvent(parts);
        } else {
            throw new InvalidMessageException("Unknown message kind: " + kind);
        }
    }

    private Reading parseReading(String[] p) throws InvalidMessageException {
        if (p.length != 13) {
            throw new InvalidMessageException("Telemetry needs 13 fields but has " + p.length);
        }
        String deviceId = checkId(p[1], "device id");
        String bootId = checkId(p[2], "boot id");
        int seq = parseSeq(p[3]);
        long uptime = parseUptime(p[4]);
        boolean sim = parseSim(p[5]);
        String envStatus = parseStatus(p[11]);
        String motionStatus = parseStatus(p[12]);

        double temp = 0;
        double hum = 0;
        if (envStatus.equals(Reading.OK)) {
            temp = parseNumber(p[6], "temperature");
            hum = parseNumber(p[7], "humidity");
            if (temp < -40 || temp > 85) {
                throw new InvalidMessageException("Temperature out of sensor range: " + temp);
            }
            if (hum < 0 || hum > 100) {
                throw new InvalidMessageException("Humidity must be 0 to 100: " + hum);
            }
        } else if (!p[6].equals("NA") || !p[7].equals("NA")) {
            throw new InvalidMessageException("A faulty environment sensor must send NA values");
        }

        double ax = 0, ay = 0, az = 0;
        if (motionStatus.equals(Reading.OK)) {
            ax = parseNumber(p[8], "ax");
            ay = parseNumber(p[9], "ay");
            az = parseNumber(p[10], "az");
        } else if (!p[8].equals("NA") || !p[9].equals("NA") || !p[10].equals("NA")) {
            throw new InvalidMessageException("A faulty motion sensor must send NA values");
        }
        return new Reading(deviceId, bootId, seq, uptime, sim, temp, hum, ax, ay, az,
                envStatus, motionStatus);
    }

    private HelmetEvent parseEvent(String[] p) throws InvalidMessageException {
        if (p.length != 7) {
            throw new InvalidMessageException("Event needs 7 fields but has " + p.length);
        }
        String deviceId = checkId(p[1], "device id");
        String bootId = checkId(p[2], "boot id");
        int seq = parseSeq(p[3]);
        long uptime = parseUptime(p[4]);
        boolean sim = parseSim(p[5]);
        String type = p[6];
        if (!type.equals(HelmetEvent.SOS) && !type.equals(HelmetEvent.MOTION)) {
            throw new InvalidMessageException("Unknown event type: " + type);
        }
        return new HelmetEvent(deviceId, bootId, seq, uptime, sim, type);
    }

    private String checkId(String text, String name) throws InvalidMessageException {
        if (text.isEmpty() || text.contains("/") || text.contains(" ")) {
            throw new InvalidMessageException("Invalid " + name + ": '" + text + "'");
        }
        return text;
    }

    private int parseSeq(String text) throws InvalidMessageException {
        try {
            int seq = Integer.parseInt(text);
            if (seq < 0) {
                throw new InvalidMessageException("Sequence number cannot be negative");
            }
            return seq;
        } catch (NumberFormatException e) {
            throw new InvalidMessageException("Sequence number is not a whole number: " + text);
        }
    }

    private long parseUptime(String text) throws InvalidMessageException {
        try {
            long value = Long.parseLong(text);
            if (value < 0) {
                throw new InvalidMessageException("Uptime cannot be negative");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidMessageException("Uptime is not a number: " + text);
        }
    }

    private boolean parseSim(String text) throws InvalidMessageException {
        if (text.equals("1")) {
            return true;
        } else if (text.equals("0")) {
            return false;
        }
        throw new InvalidMessageException("Simulated flag must be 0 or 1: " + text);
    }

    private String parseStatus(String text) throws InvalidMessageException {
        if (text.equals(Reading.OK) || text.equals(Reading.FAULT)) {
            return text;
        }
        throw new InvalidMessageException("Sensor status must be ok or fault: " + text);
    }

    private double parseNumber(String text, String name) throws InvalidMessageException {
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new InvalidMessageException(name + " is not a real number");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidMessageException(name + " is not a number: " + text);
        }
    }
}
