package csv.lab;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import csv.TimeAdjustable;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class LaboratoryData implements TimeAdjustable<LaboratoryData> {

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

    @Override
    public LaboratoryData offsetTimesBySeconds(long offsetSeconds) {
        return LaboratoryData.builder()
                .documentId(documentId)
                .documentVersion(documentVersion)
                .documentPart(documentPart)
                .sequenceId(sequenceId)
                .medicalCaseId(medicalCaseId)
                .patientId(patientId)
                .orderId(orderId)
                .createdAt(createdAt.plusSeconds(offsetSeconds)) // adjust
                .originatingSystem(originatingSystem)
                .analyte(analyte)
                .analyteText(analyteText)
                .analyteValue(analyteValue)
                .analyteUnit(analyteUnit)
                .measuredAt(measuredAt.plusSeconds(offsetSeconds)) // adjust
                .build();
    }

    public LaboratoryMessage convertToLaboratoryMessage() {
        return new LaboratoryMessage(
                documentId,
                documentVersion,
                documentPart,
                sequenceId,
                medicalCaseId,
                patientId,
                orderId,
                createdAt,
                originatingSystem,
                analyte,
                analyteValue,
                analyteUnit,
                measuredAt
        );
    }

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
