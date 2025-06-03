package csv.procedure;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public final class ProcedureData {

    @CsvBindByName(column = "procedureId")
    private String id;

    @CsvBindByName(column = "caseId")
    private String medicalCaseId;

    @CsvBindByName(column = "procedureCode")
    private String code;

    @CsvBindByName(column = "performedOnTimestamp")
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime performedOn;

    @CsvBindByName(column = "cancellationDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate cancellationDate;

    @CsvBindByName(column = "updateDate")
    @CsvDate(value = "yyyy-MM-dd")
    private LocalDate updateDate;

    public String toString() {
        return "ID: %s | Medical Case ID: %s | Code: %s | Performed On: %s | Cancellation Date: %s | Update Date: %s"
                .formatted(
                        getId(),
                        getMedicalCaseId(),
                        getCode(),
                        getPerformedOn(),
                        getCancellationDate(),
                        getUpdateDate()
                );
    }
}
