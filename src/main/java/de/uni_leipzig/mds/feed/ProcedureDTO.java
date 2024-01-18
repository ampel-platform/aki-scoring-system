package de.uni_leipzig.mds.feed;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDateTime;

@Getter
public final class ProcedureDTO {

    @BsonProperty(value = "conductTime")
    private final LocalDateTime time;
    private final String code;

    @BsonCreator
    public ProcedureDTO(@BsonProperty(value = "conductTime") LocalDateTime time,
                        @BsonProperty(value = "code") String code) {
        this.time = time;
        this.code = code;
    }

    ProcedureDTO(Procedure entity) {
        this(entity.getTime(), entity.getCode());
    }
}
