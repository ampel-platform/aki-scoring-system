package de.uni_leipzig.mds.processing.context;

import java.time.LocalDateTime;

public record WorkflowLabValue(
        String orderId,
        String analyte,
        double analyteValue,
        String analyteUnit,
        String originatingSystem,
        LocalDateTime measuringTime
) {
}
