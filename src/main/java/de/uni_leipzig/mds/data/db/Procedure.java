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
 * Describes a single procedure and its representation within the database.
 */
@Getter
@EqualsAndHashCode
public final class Procedure {
    @BsonProperty(value = "_id")
    private final ObjectId id;
    @BsonProperty(value = "conductTime")
    private final LocalDateTime time;
    private final String code;
    private final String patientIdRef;
    private final String caseIdRef;

    @BsonCreator
    public Procedure(@BsonProperty(value = "_id") ObjectId id,
              @BsonProperty(value = "conductTime") LocalDateTime time,
              @BsonProperty(value = "code") String code,
              @BsonProperty(value = "patientIdRef") String patientIdRef,
              @BsonProperty(value = "caseIdRef") String caseIdRef
    ) {
        this.id = id;
        this.time = time;
        this.code = code;
        this.patientIdRef = patientIdRef;
        this.caseIdRef = caseIdRef;


//        requireNonNull(time, "time must not be null");
//        this.time = time.truncatedTo(ChronoUnit.MILLIS);
//
//        requireNonNull(code, "code must not be null");
//        if (code.isBlank()) {
//            throw new IllegalArgumentException("code must not be blank");
//        }
//        this.code = code;
    }

    private Procedure(ObjectId id, Builder.FinalStage builder) {
        this.id = id;
        this.time = builder.time;
        this.code = builder.code;
        this.patientIdRef = builder.patientIdRef;
        this.caseIdRef = builder.caseIdRef;
    }

    @FunctionalInterface
    public interface RequireTime<T> {
        T time(LocalDateTime time);
    }

    @FunctionalInterface
    public interface RequireCode<T> {
        T code(String code);
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
    public interface RequireRefs<T> extends RequirePatientRef<RequireCaseRef<T>> {
        default T refs(String patientId, String caseId) {
            return patientRef(patientId)
                    .caseRef(caseId);
        }
    }

    public static class Builder {

        @AllArgsConstructor
        public static class FinalStage {
            private LocalDateTime time;
            private String code;
            private String patientIdRef;
            private String caseIdRef;

            public Procedure build() {
                return new Procedure(ObjectId.get(), this);
            }

            // TODO: add validation
        }

        public static RequireTime<RequireCode<RequireRefs<FinalStage>>> builder() {
            return time -> code -> patientId -> caseId -> new FinalStage(
                    time.truncatedTo(ChronoUnit.MILLIS),
                    code,
                    patientId,
                    caseId);
        }
    }
}
