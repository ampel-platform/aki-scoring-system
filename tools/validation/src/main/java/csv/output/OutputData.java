package csv.output;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;

@Getter
public final class OutputData {

    @CsvBindByName(column = "caseId")
    private String medicalCaseId;

    @CsvBindByName(column = "orderId")
    private String orderId;

    @CsvBindByName(column = "resultAmpel")
    private String severityRepresentation;

    public String toString() {
        return "Medical Case Id: %s | Order Id: %s | Severity: %s"
                .formatted(
                        getMedicalCaseId(),
                        getOrderId(),
                        getSeverityRepresentation()
                );
    }
}
