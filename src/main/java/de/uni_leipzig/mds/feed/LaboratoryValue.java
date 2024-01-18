package de.uni_leipzig.mds.feed;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public final class LaboratoryValue {
    private final LocalDateTime time;
    private final double value;
    private final String unit;

    LaboratoryValue(LocalDateTime time, double value, String unit) {
        this.time = requireNonNull(time, "time must not be null");
        this.value = value;

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        this.unit = unit;
    }

    LaboratoryValue(LaboratoryValueDTO dto) {
        this.time = dto.getTime();
        this.value = dto.getValue();
        this.unit = dto.getUnit();
    }
}
