package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

public record MedicalCase(
        // let MongoDB use its own internal ObjectId for references.
        String medicalCaseId,
        String patientIdRef,
        Instant creationDate,
        Instant cancellationDate,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        Instant dbIngestionTime
) {
    public MedicalCase(String id,
                       String patientIdRef,
                       Instant creationDate,
                       Instant cancellationDate) {
        this(id, patientIdRef, creationDate, cancellationDate, null);
    }

    public MedicalCase(MedicalCase medicalCase, Instant ingestionTime) {
        this(medicalCase.medicalCaseId(), medicalCase.patientIdRef, medicalCase.creationDate, medicalCase.cancellationDate,
                ingestionTime);
    }
}
