package csv.procedure;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
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

    public ProcedureMessage convertToProcedureMessage() {
        return new ProcedureMessage(
                id,
                medicalCaseId,
                code,
                performedOn.toInstant(ZoneOffset.UTC),
                (cancellationDate != null)
                        ? Optional.of(cancellationDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty(),
                (updateDate != null)
                        ? Optional.of(updateDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                        : Optional.empty()
        );
    }

    public Instant performedOn() {
        return performedOn.toInstant(ZoneOffset.UTC);
    }

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
