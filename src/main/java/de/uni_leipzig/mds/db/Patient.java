package de.uni_leipzig.mds.db;

import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Patient(
        @BsonId
        String id,
        LocalDate dateOfBirth,
        Boolean deceased,
        LocalDateTime dischargeTime
) {
}
