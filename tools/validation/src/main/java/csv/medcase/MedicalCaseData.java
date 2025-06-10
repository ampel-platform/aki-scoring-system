package csv.medcase;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import csv.TimeAdjustable;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class MedicalCaseData implements TimeAdjustable<MedicalCaseData> {

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

    @Override
    public MedicalCaseData offsetTimesBySeconds(long offsetSeconds) {
        return MedicalCaseData.builder()
                .id(id)
                .patientId(patientId)
                .status(status)
                .creationDate(creationDate.atStartOfDay().plusSeconds(offsetSeconds).toLocalDate())
                .cancellationDate(cancellationDate.atStartOfDay().plusSeconds(offsetSeconds).toLocalDate())
                .updateDate(updateDate.atStartOfDay().plusSeconds(offsetSeconds).toLocalDate())
                .build();
    }

    public MedCaseMessage convertToMedicalCaseMessage() {
        return new MedCaseMessage(
                id,
                patientId,
                mapMedicalCaseStatus(),
                creationDate,
                cancellationDate,
                updateDate
        );
    }

    private MedCaseStatus mapMedicalCaseStatus() {
        return switch (status) {
            case ACTIVE -> MedCaseStatus.ACTIVE;
            case COMPLETED -> MedCaseStatus.COMPLETED;
            case PLANNED -> MedCaseStatus.PLANNED;
        };
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
