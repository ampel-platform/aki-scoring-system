package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Procedure(
        @BsonId
        ObjectId id,
        String caseId,
        String procedureId,
        String procedureCode,
        LocalDateTime conductTime,
        LocalDate cancellationTime
) {
}
