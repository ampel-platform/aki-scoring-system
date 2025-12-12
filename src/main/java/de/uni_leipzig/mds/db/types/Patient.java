package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Patient(
        // let MongoDB use its own internal ObjectId for references.
        String patientId,
        String firstname,
        String lastname,
        LocalDate dateOfBirth,
        Boolean deceased,
        Instant dischargeTime,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        LocalDateTime dbIngestionTime
) {

    public Patient(String id,
                   String firstname,
                   String lastname,
                   LocalDate dateOfBirth,
                   Boolean deceased,
                   Instant dischargeTime) {
        this(id, firstname, lastname, dateOfBirth, deceased, dischargeTime, null);
    }

    public Patient(Patient patient, Instant ingestionTime) {
        this(patient.patientId, patient.firstname, patient.lastname, patient.dateOfBirth, patient.deceased,
                ingestionTime);
    }
}
