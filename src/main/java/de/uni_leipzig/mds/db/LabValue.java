package de.uni_leipzig.mds.db;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public record LabValue(
        @BsonId
        ObjectId id,
        String caseId,
        String orderId,
        String analyte,
        double analyteValue,
        String analyteUnit,
        String originatingSystem,
        LocalDateTime measuringTime
) {
}
