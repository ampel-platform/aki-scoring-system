package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Patient(
        // let MongoDB use its own internal ObjectId for references.
        String patientId,
        LocalDate dateOfBirth,
        Boolean deceased,
        LocalDateTime dischargeTime,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        LocalDateTime dbIngestionTime
) {

    public Patient(String id,
                   LocalDate dateOfBirth,
                   Boolean deceased,
                   LocalDateTime dischargeTime) {
        this(id, dateOfBirth, deceased, dischargeTime, null);
    }

    public Patient(Patient patient, LocalDateTime ingestionTime) {
        this(patient.patientId, patient.dateOfBirth, patient.deceased, ingestionTime);
    }
}
