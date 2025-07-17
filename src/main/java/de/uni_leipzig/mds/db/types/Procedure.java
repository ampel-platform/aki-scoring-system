package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Procedure(
        // let MongoDB use its own internal ObjectId for references.
        String medicalCaseId,
        String procedureId,
        String procedureCode,
        LocalDateTime conductTime,
        LocalDate cancellationTime,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        LocalDateTime dbIngestionTime
) {
    public Procedure(String caseId,
                     String procedureId,
                     String procedureCode,
                     LocalDateTime conductTime,
                     LocalDate cancellationTime) {
        this(caseId, procedureId, procedureCode, conductTime, cancellationTime, null);
    }

    public Procedure(Procedure procedure, LocalDateTime ingestionTime) {
        this(procedure.medicalCaseId, procedure.procedureId, procedure.procedureCode, procedure.conductTime,
                procedure.cancellationTime, ingestionTime);
    }
}
