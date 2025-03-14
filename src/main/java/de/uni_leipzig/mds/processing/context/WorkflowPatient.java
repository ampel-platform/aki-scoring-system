package de.uni_leipzig.mds.processing.context;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkflowPatient(
        String id,
        LocalDate dateOfBirth,
        boolean deceased,
        LocalDateTime dischargeTime
) {
}
