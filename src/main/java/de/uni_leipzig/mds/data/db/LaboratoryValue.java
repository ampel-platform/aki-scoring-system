package de.uni_leipzig.mds.data.db;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Describes a single laboratory value and its representation within the database.
 */
@Getter
@EqualsAndHashCode
public final class LaboratoryValue {
    @BsonProperty(value = "_id")
    private final ObjectId id;
    private final String analyteCode;
    private final Double value;
    private final String unit;
    @BsonProperty(value = "measurementTime")
    private final LocalDateTime time;
    private final String orderIdRef;
    private final String patientIdRef;
    private final String caseIdRef;

    @BsonCreator
    public LaboratoryValue(@BsonProperty(value = "_id") ObjectId id,
                    @BsonProperty(value = "analyteCode") String analyteCode,
                    @BsonProperty(value = "value") Double value,
                    @BsonProperty(value = "unit") String unit,
                    @BsonProperty(value = "measurementTime") LocalDateTime time,
                    @BsonProperty(value = "orderIdRef") String orderIdRef,
                    @BsonProperty(value = "patientIdRef") String patientIdRef,
                    @BsonProperty(value = "caseIdRef") String caseIdRef
    ) {
        this.id = id;
        this.analyteCode = analyteCode;
        this.value = value;
        this.unit = unit;
        this.time = time;
        this.orderIdRef = orderIdRef;
        this.patientIdRef = patientIdRef;
        this.caseIdRef = caseIdRef;


        // TODO: validation needs to go to the builder - this is coming from the database so nothing is about to happen
//        this.id = requireNonNull(id, "id must not be null");
//
//        requireNonNull(time, "time must not be null");
//        this.time = time.truncatedTo(ChronoUnit.MILLIS);
//
//        this.value = value;
//
//        requireNonNull(unit, "unit must not be null");
//        if (unit.isBlank()) {
//            throw new IllegalArgumentException("unit must not be blank");
//        }
//        this.unit = unit;
    }

    private LaboratoryValue(ObjectId id, Builder.FinalStage builder) {
        this.id = id;
        this.analyteCode = builder.analyteCode;
        this.value = builder.value;
        this.unit = builder.unit;
        this.time = builder.time;
        this.orderIdRef = builder.orderIdRef;
        this.patientIdRef = builder.patientIdRef;
        this.caseIdRef = builder.caseIdRef;
    }

    @FunctionalInterface
    public interface RequireTime<T> {
        T time(LocalDateTime time);
    }

    @FunctionalInterface
    public interface RequireAnalyteCode<T> {
        T code(String code);
    }

    @FunctionalInterface
    public interface RequireValue<T> {
        T value(Double value);
    }

    @FunctionalInterface
    public interface RequireUnit<T> {
        T unit(String unit);
    }

    @FunctionalInterface
    public interface RequireOrderRef<T> {
        T orderRef(String orderId);
    }

    @FunctionalInterface
    public interface RequirePatientRef<T> {
        T patientRef(String patientId);
    }

    @FunctionalInterface
    public interface RequireCaseRef<T> {
        T caseRef(String caseId);
    }

    @FunctionalInterface
    public interface RequireRefs<T> extends RequireOrderRef<RequirePatientRef<RequireCaseRef<T>>> {
        default T refs(String orderId, String patientId, String caseId) {
            return orderRef(orderId)
                    .patientRef(patientId)
                    .caseRef(caseId);
        }
    }


    public static class Builder {

        @AllArgsConstructor
        public static class FinalStage {
            private String analyteCode;
            private Double value;
            private String unit;
            private LocalDateTime time;
            private String orderIdRef;
            private String patientIdRef;
            private String caseIdRef;

            public LaboratoryValue build() {
                return new LaboratoryValue(ObjectId.get(), this);
            }

            // TODO: add validation
        }

        public static RequireAnalyteCode<RequireValue<RequireUnit<RequireTime<RequireRefs<FinalStage>>>>>
        builder() {
            return analyteCode -> value -> unit -> time -> orderId -> patientId -> caseId ->
                    new Builder.FinalStage(
                            analyteCode,
                            value,
                            unit,
                            time.truncatedTo(ChronoUnit.MILLIS),
                            orderId,
                            patientId,
                            caseId);
        }
    }
}
