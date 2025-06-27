package csv.output;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;

@Getter
public final class OutputData {

    @CsvBindByName(column = "X.BCX.S_HC_FALNR")
    private String medicalCaseId;

    @CsvBindByName(column = "X.BCX.S_HC_LA_ANR")
    private String orderId;

    @CsvBindByName(column = "X.BCX.S_HC_RGL_KS")
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
