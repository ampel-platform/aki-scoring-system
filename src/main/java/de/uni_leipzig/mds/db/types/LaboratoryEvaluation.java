package de.uni_leipzig.mds.db.types;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.Instant;

public record LaboratoryEvaluation(
        // let MongoDB use its own internal ObjectId for references.
        String medicalCaseId,
        String orderId,
        String analyte,
        double analyteValue,
        String analyteUnit,
        String originatingSystem,
        Instant measuringTime,

        // Necessary for dealing with duplicates since we are not doing upserts anymore.
        // The latest entry (associated with a specific id) will also have the latest ingestion time.
        @BsonProperty(value = "_ingestionTime")
        Instant dbIngestionTime
) {

    public LaboratoryEvaluation(String caseId,
                                String orderId,
                                String analyte,
                                double analyteValue,
                                String analyteUnit,
                                String originatingSystem,
                                Instant measuringTime) {
        this(caseId, orderId, analyte, analyteValue, analyteUnit, originatingSystem, measuringTime, null);
    }

    public LaboratoryEvaluation(LaboratoryEvaluation entry, Instant ingestionTime) {
        this(entry.medicalCaseId, entry.orderId, entry.analyte, entry.analyteValue, entry.analyteUnit, entry.originatingSystem,
                entry.measuringTime, ingestionTime);
    }
}
