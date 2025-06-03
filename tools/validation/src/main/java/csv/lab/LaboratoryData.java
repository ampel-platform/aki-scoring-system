package csv.lab;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class LaboratoryData {

    @CsvBindByName(column = "documentId")
    private String documentId;

    @CsvBindByName(column = "documentVersion")
    private String documentVersion;

    @CsvBindByName(column = "documentPart")
    private String documentPart;

    @CsvBindByName(column = "sequenceId")
    private String sequenceId;

    @CsvBindByName(column = "caseId")
    private String medicalCaseId;

    @CsvBindByName(column = "patientId")
    private String patientId;

    @CsvBindByName(column = "orderId")
    private String orderId;

    @CsvBindByName(column = "documentCreationTimestamp")
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @CsvBindByName(column = "originatingSystem")
    private String originatingSystem;

    @CsvBindByName(column = "analyte")
    private String analyte;

    @CsvBindByName(column = "analyteText")
    private String analyteText;

    @CsvCustomBindByName(column = "analyteValue", converter = AnalyteValueConverter.class)
    private Float analyteValue;

    @CsvBindByName(column = "analyteUnit")
    private String analyteUnit;

    @CsvBindByName(column = "analyteMeasuringTimestamp")
    @CsvDate(value = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    public String toString() {
        return "ID: %s | Ver: %s | Part: %s | Seq: %s | Medical Case ID: %s | Patient ID: %s | Order ID: %s | Creation: %s | Originating System: %s | Analyte: %s | Analyte Text: %s | Analyte Value: %f | Analyte Unit: %s | Measured At: %s"
                .formatted(
                        getDocumentId(),
                        getDocumentVersion(),
                        getDocumentPart(),
                        getSequenceId(),
                        getMedicalCaseId(),
                        getPatientId(),
                        getOrderId(),
                        getCreatedAt(),
                        getOriginatingSystem(),
                        getAnalyte(),
                        getAnalyteText(),
                        getAnalyteValue(),
                        getAnalyteUnit(),
                        getMeasuredAt()
                );
    }
}
