package de.uni_leipzig.mds.feed;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Date;

@Data
@NoArgsConstructor
public class ProcedureDTO {

    @BsonProperty(value = "conduct_time")
    private Date time;

    private String code;

    ProcedureDTO(ProcedureDTO source) {
        this.time = new Date(source.getTime().getTime());
        this.code = source.getCode();
    }
}
