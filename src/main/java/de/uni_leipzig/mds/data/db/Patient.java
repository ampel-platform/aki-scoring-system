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
 * Describes a single patient and its representation within the database.
 */
@Getter
@EqualsAndHashCode
public final class Patient {
    @BsonProperty(value = "_id")
    private final ObjectId id;
    @BsonProperty(value = "patientId")
    private final String patientId;
    private final LocalDateTime dateOfBirth;
    private final boolean deceased;

    @BsonCreator
    public Patient(@BsonProperty(value = "_id") ObjectId id,
            @BsonProperty(value = "patientId") String patientId,
            @BsonProperty(value = "dateOfBirth") LocalDateTime dateOfBirth,
            @BsonProperty(value = "deceased") boolean deceased) {
//        requireNonNull(id, "id must not be null");
//        if (id.isBlank()) {
//            throw new IllegalArgumentException("id must not be blank");
//        }
//        this.id = id;
//
//        requireNonNull(dateOfBirth, "date of birth must not be null");
//        this.dateOfBirth = dateOfBirth.truncatedTo(ChronoUnit.MILLIS);
//
//        this.deceased = deceased;
        this.id = id;
        this.patientId = patientId;
        this.dateOfBirth = dateOfBirth;
        this.deceased = deceased;
    }

    private Patient(ObjectId id, Builder.FinalStage builder) {
        this.id = id;
        this.patientId = builder.patientId;
        this.dateOfBirth = builder.dateOfBirth;
        this.deceased = builder.deceased;
    }

    @FunctionalInterface
    public interface RequirePatientRef<T> {
        T patientRef(String patientId);
    }

    @FunctionalInterface
    public interface RequireDateOfBirth<T> {
        T dateOfBirth(LocalDateTime dateOfBirth);
    }

    @FunctionalInterface
    public interface RequireVitalStatus<T> {
        T deceased(boolean deceased);
    }

    public static class Builder {

        @AllArgsConstructor
        public static class FinalStage {
            private String patientId;
            private LocalDateTime dateOfBirth;
            private boolean deceased;

            public Patient build() {
                return new Patient(ObjectId.get(), this);
            }

            // TODO: add validation
        }

        public static RequirePatientRef<RequireDateOfBirth<RequireVitalStatus<FinalStage>>> builder() {
            return patientId -> dateOfBirth -> deceased -> new FinalStage(
                    patientId,
                    dateOfBirth.truncatedTo(ChronoUnit.MILLIS),
                    deceased);
        }
    }
}
