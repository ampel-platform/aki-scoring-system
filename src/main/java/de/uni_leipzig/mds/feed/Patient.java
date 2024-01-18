package de.uni_leipzig.mds.feed;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public final class Patient {

    private final String id;
    private final LocalDateTime dateOfBirth;
    private final boolean deceased;

    Patient(String id, LocalDateTime dateOfBirth, boolean deceased) {
        requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.id = id;

        requireNonNull(dateOfBirth, "date of birth must not be null");
        this.dateOfBirth = dateOfBirth.truncatedTo(ChronoUnit.MILLIS);

        this.deceased = deceased;
    }

    Patient(PatientDTO dto) {
        this.id = dto.getId();
        this.dateOfBirth = dto.getDateOfBirth();
        this.deceased = dto.isDeceased();
    }
}
