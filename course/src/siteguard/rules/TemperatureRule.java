package siteguard.rules;

import siteguard.model.Incident;
import siteguard.model.Reading;

public class TemperatureRule extends ThresholdRule {

    public TemperatureRule() {
        super("High temperature", Incident.MEDIUM);
    }

    @Override
    protected double value(Reading reading) {
        return reading.getTemperatureC();
    }

    @Override
    protected double limit(Thresholds thresholds) {
        return thresholds.getMaxTemperatureC();
    }
}
