package de.uni_leipzig.mds.feed;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Date;

import static java.util.Objects.requireNonNull;

@Data
@NoArgsConstructor
public class PatientDTO {

    @BsonProperty(value = "patient_id")
    private String id;

    @BsonProperty(value = "date_of_birth")
    private Date dateOfBirth;

    private Boolean deceased;

    PatientDTO(PatientDTO source) {
        this.id = source.getId();
        this.deceased = source.getDeceased();
        this.dateOfBirth = new Date(source.getDateOfBirth().getTime());
    }
}
