package de.uni_leipzig.mds.processing.context;

import java.time.Instant;

public record WorkflowProcedure(
        String procedureId,
        String procedureCode,
        Instant conductTime,
        Instant cancellationTime
) {
}
