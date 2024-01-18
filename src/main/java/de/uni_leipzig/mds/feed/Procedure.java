package de.uni_leipzig.mds.feed;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public final class Procedure {
    private final LocalDateTime time;
    private final String code;

    Procedure(Procedure original) {
        this.time = original.getTime();
        this.code = original.getCode();
    }

    Procedure(LocalDateTime time, String code) {
        this.time = requireNonNull(time, "time must not be null");

        requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        this.code = code;
    }

    Procedure(ProcedureDTO dto) {
        this.time = dto.getTime();
        this.code = dto.getCode();
    }
}
