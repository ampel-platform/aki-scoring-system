package de.uni_leipzig.mds.feed;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Date;

@Data
@NoArgsConstructor
public class LaboratoryValueDTO {

    @BsonProperty(value = "measurement_time")
    private Date time;

    private double value;

    private String unit;

    LaboratoryValueDTO(LaboratoryValueDTO source) {
        this.time = new Date(source.getTime().getTime());
        this.value = source.getValue();
        this.unit = source.getUnit();
    }
}
