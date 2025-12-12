package de.uni_leipzig.mds.processing.context;

import java.time.Instant;
import java.time.LocalDate;

public record WorkflowPatient(
        String id,
        LocalDate dateOfBirth,
        boolean deceased,
        Instant dischargeTime
) {
}
