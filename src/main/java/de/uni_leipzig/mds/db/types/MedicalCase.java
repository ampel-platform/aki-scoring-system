package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalCase(
        // let MongoDB use its own internal ObjectId for references.
        String medicalCaseId,
        String patientIdRef,
        LocalDate creationDate,
        LocalDate cancellationDate,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        LocalDateTime dbIngestionTime
) {
    public MedicalCase(String id,
                       String patientIdRef,
                       LocalDate creationDate,
                       LocalDate cancellationDate) {
        this(id, patientIdRef, creationDate, cancellationDate, null);
    }

    public MedicalCase(MedicalCase medicalCase, LocalDateTime ingestionTime) {
        this(medicalCase.medicalCaseId(), medicalCase.patientIdRef, medicalCase.creationDate, medicalCase.cancellationDate,
                ingestionTime);
    }
}
