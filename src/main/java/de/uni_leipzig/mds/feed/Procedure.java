package de.uni_leipzig.mds.feed;

import lombok.Getter;

import java.util.Date;

import static java.util.Objects.requireNonNull;

public final class Procedure {
    private final Date time;

    @Getter
    private final String code;

    Procedure(Procedure original) {
        this.time = original.getTime();
        this.code = original.getCode();
    }

    Procedure(Date time, String code) {
        this.time = requireNonNull(time, "time must not be null");

        requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = code;
    }

    public Date getTime() {
        return new Date(time.getTime());
    }
}
