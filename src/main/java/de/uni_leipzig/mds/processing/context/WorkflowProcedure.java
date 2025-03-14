package de.uni_leipzig.mds.processing.context;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkflowProcedure(
        String procedureId,
        String procedureCode,
        LocalDateTime conductTime,
        LocalDate cancellationTime
) {
}
