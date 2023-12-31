package de.uni_leipzig.mds.feed;

import lombok.Getter;

import java.util.Date;

import static java.util.Objects.requireNonNull;

public final class Patient {

    @Getter
    private final String id;
    private final Date dateOfBirth;

    @Getter
    private final boolean deceased;

    Patient(String id, Date dateOfBirth, boolean deceased) {
        requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.id = id;

        this.dateOfBirth = requireNonNull(dateOfBirth, "date of birth must not be null");
        this.deceased = deceased;
    }

    public Date getDateOfBirth() {
        return new Date(dateOfBirth.getTime());
    }
}
