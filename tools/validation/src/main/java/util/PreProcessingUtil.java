package util;

import csv.lab.LaboratoryData;
import csv.lab.LaboratoryDataMeasuringTimestampComparator;
import csv.medcase.MedicalCaseData;
import csv.medcase.MedicalCaseDataCreationComparator;
import csv.patient.PatientData;
import csv.patient.PatientDataCreationComparator;
import csv.procedure.ProcedureData;
import csv.procedure.ProcedureDataTimeComparator;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public final class PreProcessingUtil {

    private PreProcessingUtil() {
    }

    public static LocalDateTime getLatestTimestamp(
            List<PatientData> patients,
            List<LaboratoryData> labData,
            List<MedicalCaseData> medicalCases,
            List<ProcedureData> procedures
    ) {
        var sortedPatients = sortDescByTime(patients, new PatientDataCreationComparator());
        var sortedLabData = sortDescByTime(labData, new LaboratoryDataMeasuringTimestampComparator());
        var sortedMedicalCases = sortDescByTime(medicalCases, new MedicalCaseDataCreationComparator());
        var sortedProcedures = sortDescByTime(procedures, new ProcedureDataTimeComparator());

        var latestTimestamp = LocalDateTime.MIN;

        if (!sortedPatients.isEmpty()
                && sortedPatients.getFirst().getCreationDate().atStartOfDay().isAfter(latestTimestamp)
        ) {
            latestTimestamp = sortedPatients.getFirst().getCreationDate().atStartOfDay();
        }

        if (!sortedLabData.isEmpty()
                && sortedLabData.getFirst().getMeasuredAt().isAfter(latestTimestamp)
        ) {
            latestTimestamp = sortedLabData.getFirst().getMeasuredAt();
        }

        if (!sortedMedicalCases.isEmpty()
                && sortedMedicalCases.getFirst().getCreationDate().atStartOfDay().isAfter(latestTimestamp)
        ) {
            latestTimestamp = sortedMedicalCases.getFirst().getCreationDate().atStartOfDay();
        }

        if (!sortedProcedures.isEmpty()
                && sortedProcedures.getFirst().getPerformedOn().isAfter(latestTimestamp)
        ) {
            latestTimestamp = sortedProcedures.getFirst().getPerformedOn();
        }

        return latestTimestamp;
    }

    public static List<LaboratoryData> filterForNonNaNValues(List<LaboratoryData> labData) {
        return labData.stream()
                .filter(ld -> !ld.getAnalyteValue().isNaN())
                .toList();
    }

    public static List<ProcedureData> filterForProceduresNotOlderThan7Days(List<ProcedureData> procedures,
                                                                           LocalDateTime referenceTimestamp) {
        return procedures.stream()
                .filter(p -> Math.abs(ChronoUnit.DAYS.between(p.getPerformedOn(), referenceTimestamp)) <= 7)
                .toList();
    }

    public static List<LaboratoryData> filterForLabValuesNotOlderThan7Days(List<LaboratoryData> labData,
                                                                           LocalDateTime referenceTimestamp) {
        return labData.stream()
                .filter(l -> Math.abs(ChronoUnit.DAYS.between(l.getMeasuredAt(), referenceTimestamp)) <= 7)
                .toList();
    }

    private static <T> List<T> sortDescByTime(List<T> list, Comparator<T> comparator) {
        return list.stream().sorted(comparator.reversed()).toList();
    }
}
