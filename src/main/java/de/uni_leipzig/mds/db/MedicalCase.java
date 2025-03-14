package de.uni_leipzig.mds.db;

import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDate;

public record MedicalCase(
        @BsonId
        String id,
        String patientIdRef,
        LocalDate creationDate,
        LocalDate cancellationDate
) {
}
