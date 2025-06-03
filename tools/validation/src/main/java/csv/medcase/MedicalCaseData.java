package csv.medcase;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Getter;

import java.time.LocalDate;

@Getter
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
