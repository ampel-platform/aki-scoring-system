package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDateTime;

public record LaboratoryEvaluation(
        // let MongoDB use its own internal ObjectId for references.
        String medicalCaseId,
        String orderId,
        String analyte,
        double analyteValue,
        String analyteUnit,
        String originatingSystem,
        LocalDateTime measuringTime,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        LocalDateTime dbIngestionTime
) {

    public LaboratoryEvaluation(String caseId,
                                String orderId,
                                String analyte,
                                double analyteValue,
                                String analyteUnit,
                                String originatingSystem,
                                LocalDateTime measuringTime) {
        this(caseId, orderId, analyte, analyteValue, analyteUnit, originatingSystem, measuringTime, null);
    }

    public LaboratoryEvaluation(LaboratoryEvaluation entry, LocalDateTime ingestionTime) {
        this(entry.medicalCaseId, entry.orderId, entry.analyte, entry.analyteValue, entry.analyteUnit, entry.originatingSystem,
                entry.measuringTime, ingestionTime);
    }
}
