package csv.patient;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.ampel.sdk.io.message.patient.Sex;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class PatientData {

    @CsvBindByName(column = "patientId")
    private String id;

    @CsvCustomBindByName(column = "sex", converter = PatientSexConverter.class)
    private PatientSex sex;

    @CsvBindByName(column = "dateOfBirth")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @CsvCustomBindByName(column = "deceased", converter = PatientDeceasedConverter.class)
    private Boolean deceased;

    @CsvBindByName(column = "dischargeTimestamp")
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dischargeTimestamp;

    @CsvBindByName(column = "cancellationDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate cancellationDate;

    @CsvBindByName(column = "creationDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate creationDate;

    @CsvBindByName(column = "updateDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate updateDate;

    public PatientMessage convertToPatientMessage() {
        return new PatientMessage(
                id,
                mapPatientSex(),
                dateOfBirth,
                deceased,
                creationDate,
                dischargeTimestamp,
                cancellationDate,
                updateDate
        );
    }

    private Sex mapPatientSex() {
        return switch (sex) {
            case FEMALE -> Sex.FEMALE;
            case MALE -> Sex.MALE;
            default -> Sex.UNKNOWN;
        };
    }

    public String toString() {
        return "ID: %s | Sex: %s | DOB: %s | Deceased: %s | Discharge Time: %s | Cancellation Date: %s | Creation Date: %s | Update Date: %s"
                .formatted(
                        getId()
                        , getSex()
                        , getDateOfBirth()
                        , getDeceased()
                        , getDischargeTimestamp()
                        , getCancellationDate()
                        , getCreationDate()
                        , getUpdateDate()
                );
    }
}
