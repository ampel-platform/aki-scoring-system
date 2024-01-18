package de.uni_leipzig.mds.feed;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.time.LocalDateTime;

@Getter
public final class PatientDTO {

    @BsonProperty(value = "patientId")
    private final String id;
    private final LocalDateTime dateOfBirth;
    private final boolean deceased;

    @BsonCreator
    public PatientDTO(@BsonProperty(value = "patientId") String id,
                      @BsonProperty(value = "dateOfBirth") LocalDateTime dateOfBirth,
                      @BsonProperty(value = "deceased") boolean deceased) {
        this.id = id;
        this.dateOfBirth = dateOfBirth;
        this.deceased = deceased;
    }

    PatientDTO(Patient entity) {
        this(entity.getId(), entity.getDateOfBirth(), entity.isDeceased());
    }
}
