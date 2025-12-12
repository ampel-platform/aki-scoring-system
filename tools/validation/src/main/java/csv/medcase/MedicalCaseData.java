package csv.medcase;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class MedicalCaseData {

    @CsvBindByName(column = "caseId")
    private String id;

    @CsvBindByName(column = "patientId")
    private String patientId;

    @CsvCustomBindByName(column = "status", converter = MedicalCaseStatusConverter.class)
    private MedicalCaseStatus status;

    @CsvBindByName(column = "creationDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate creationDate;

    @CsvBindByName(column = "cancellationDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate cancellationDate;

    @CsvBindByName(column = "updateDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate updateDate;

    public MedCaseMessage convertToMedicalCaseMessage() {
        return new MedCaseMessage(
                id,
                patientId,
                mapMedicalCaseStatus(),
                creationDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                (cancellationDate != null)
                        ? Optional.of(cancellationDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty(),
                (updateDate != null)
                        ? Optional.of(updateDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty()
        );
    }

    private MedCaseStatus mapMedicalCaseStatus() {
        return switch (status) {
            case ACTIVE -> MedCaseStatus.ACTIVE;
            case COMPLETED -> MedCaseStatus.COMPLETED;
            case PLANNED -> MedCaseStatus.PLANNED;
        };
    }

    public Instant creationDate() {
        return creationDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public String toString() {
        return "ID: %s | Patient ID: %s | Status: %s | Creation Date: %s | Cancellation Date: %s | Update Date: %s"
                .formatted(
                        getId(),
                        getPatientId(),
                        getStatus(),
                        getCreationDate(),
                        getCancellationDate(),
                        getUpdateDate()
                );
    }
}
