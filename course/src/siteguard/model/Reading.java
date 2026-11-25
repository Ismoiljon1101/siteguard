package siteguard.model;

/**
 * One telemetry sample: temperature, humidity and acceleration.
 * When a sensor is faulty its values are not available (hasEnvironment() / hasMotion() is false).
 */
public class Reading extends Message {

    public static final String OK = "ok";
    public static final String FAULT = "fault";

    private double temperatureC;
    private double humidityPct;
    private double ax;
    private double ay;
    private double az;
    private String environmentStatus;
    private String motionStatus;

    public Reading(String deviceId, String bootId, int seq, long uptimeMs, boolean simulated,
                   double temperatureC, double humidityPct,
                   double ax, double ay, double az,
                   String environmentStatus, String motionStatus) {
        super(deviceId, bootId, seq, uptimeMs, simulated);
        this.temperatureC = temperatureC;
        this.humidityPct = humidityPct;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.environmentStatus = environmentStatus;
        this.motionStatus = motionStatus;
    }

    public boolean hasEnvironment() { return OK.equals(environmentStatus); }
    public boolean hasMotion() { return OK.equals(motionStatus); }

    /** Size of the acceleration vector in m/s^2 (about 9.8 when the helmet is still). */
    public double getAccelMagnitude() {
        return Math.sqrt(ax * ax + ay * ay + az * az);
    }

    @Override
    public String describe() {
        String env = hasEnvironment()
                ? temperatureC + " C, " + humidityPct + " %"
                : "environment sensor fault";
        String motion = hasMotion()
                ? Math.round(getAccelMagnitude() * 10) / 10.0 + " m/s2"
                : "motion sensor fault";
        return deviceId + " reading #" + seq + ": " + env + ", " + motion;
    }

    public double getTemperatureC() { return temperatureC; }
    public double getHumidityPct() { return humidityPct; }
    public double getAx() { return ax; }
    public double getAy() { return ay; }
    public double getAz() { return az; }
    public String getEnvironmentStatus() { return environmentStatus; }
    public String getMotionStatus() { return motionStatus; }
}
