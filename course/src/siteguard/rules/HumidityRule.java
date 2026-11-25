package siteguard.rules;

import siteguard.model.Incident;
import siteguard.model.Reading;

public class HumidityRule extends ThresholdRule {

    public HumidityRule() {
        super("High humidity", Incident.LOW);
    }

    @Override
    protected double value(Reading reading) {
        return reading.getHumidityPct();
    }

    @Override
    protected double limit(Thresholds thresholds) {
        return thresholds.getMaxHumidityPct();
    }
}
