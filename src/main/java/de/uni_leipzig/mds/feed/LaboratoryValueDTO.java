package de.uni_leipzig.mds.feed;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDateTime;


@Getter
public final class LaboratoryValueDTO {

    @BsonProperty(value = "measurementTime")
    private final LocalDateTime time;
    private final double value;
    private final String unit;

    @BsonCreator
    public LaboratoryValueDTO(@BsonProperty(value = "measurementTime") LocalDateTime time,
                              @BsonProperty(value = "value") double value,
                              @BsonProperty(value = "unit") String unit) {
        this.time = time;
        this.value = value;
        this.unit = unit;
    }

    LaboratoryValueDTO(LaboratoryValue entity) {
        this(entity.getTime(), entity.getValue(), entity.getUnit());
    }
}
