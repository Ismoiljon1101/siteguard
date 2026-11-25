package siteguard.rules;

/**
 * Settings the operator can change from the Settings tab.
 * These are demonstration values, not certified safety limits.
 */
public class Thresholds {

    private double maxTemperatureC = 35.0;
    private double maxHumidityPct = 85.0;
    private int sustainedSamples = 3;
    private int staleSeconds = 5;

    public double getMaxTemperatureC() { return maxTemperatureC; }
    public double getMaxHumidityPct() { return maxHumidityPct; }
    public int getSustainedSamples() { return sustainedSamples; }
    public int getStaleSeconds() { return staleSeconds; }

    public void setMaxTemperatureC(double value) {
        if (value < -40 || value > 85) {
            throw new IllegalArgumentException("Temperature limit must be between -40 and 85 C");
        }
        maxTemperatureC = value;
    }

    public void setMaxHumidityPct(double value) {
        if (value <= 0 || value > 100) {
            throw new IllegalArgumentException("Humidity limit must be between 1 and 100 %");
        }
        maxHumidityPct = value;
    }

    public void setSustainedSamples(int value) {
        if (value < 1 || value > 20) {
            throw new IllegalArgumentException("Sustained samples must be between 1 and 20");
        }
        sustainedSamples = value;
    }

    public void setStaleSeconds(int value) {
        if (value < 2 || value > 60) {
            throw new IllegalArgumentException("Stale time must be between 2 and 60 seconds");
        }
        staleSeconds = value;
    }
}
