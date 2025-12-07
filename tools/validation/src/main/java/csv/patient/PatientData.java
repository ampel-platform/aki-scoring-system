package csv.patient;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.ampel.sdk.io.message.patient.Sex;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class PatientData {

    @CsvBindByName(column = "patientId")
    private String id;

    @CsvBindByName(column = "firstname")
    private String firstname;

    @CsvBindByName(column = "lastname")
    private String lastname;

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
                firstname,
                lastname,
                dateOfBirth,
                deceased,
                creationDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                dischargeTimestamp.toInstant(ZoneOffset.UTC),
                (cancellationDate != null)
                        ? Optional.of(cancellationDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty(),
                (updateDate != null)
                        ? Optional.of(updateDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty()
        );
    }

    private Sex mapPatientSex() {
        return switch (sex) {
            case FEMALE -> Sex.FEMALE;
            case MALE -> Sex.MALE;
            default -> Sex.UNKNOWN;
        };
    }

    public Instant creationDate() {
        return creationDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public String toString() {
        return "ID: %s | Sex: %s | Firstname: %s | Lastname: %s | DOB: %s | Deceased: %s | Discharge Time: %s | Cancellation Date: %s | Creation Date: %s | Update Date: %s"
                .formatted(
                        getId()
                        , getSex()
                        , getFirstname()
                        , getLastname()
                        , getDateOfBirth()
                        , getDeceased()
                        , getDischargeTimestamp()
                        , getCancellationDate()
                        , getCreationDate()
                        , getUpdateDate()
                );
    }
}
