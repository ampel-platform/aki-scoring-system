package de.uni_leipzig.mds.processing.context;

import java.time.Instant;

public record WorkflowLabValue(
        String orderId,
        String analyte,
        double analyteValue,
        String analyteUnit,
        String originatingSystem,
        Instant measuringTime
) {
}
