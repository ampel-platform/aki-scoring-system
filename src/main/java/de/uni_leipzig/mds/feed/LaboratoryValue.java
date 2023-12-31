package de.uni_leipzig.mds.feed;

import lombok.Getter;

import java.util.Date;

import static java.util.Objects.requireNonNull;

public final class LaboratoryValue {
    private final Date time;

    @Getter
    private final double value;

    @Getter
    private final String unit;

    LaboratoryValue(LaboratoryValue original) {
        this.time = original.getTime();
        this.value = original.getValue();
        this.unit = original.getUnit();
    }

    LaboratoryValue(Date time, double value, String unit) {
        this.time = requireNonNull(time, "time must not be null");
        this.value = value;

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit must not be blank");
        }
        this.unit = unit;
    }

    public Date getTime() {
        return new Date(time.getTime());
    }
}
