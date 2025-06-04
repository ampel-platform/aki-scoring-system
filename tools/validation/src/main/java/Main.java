import csv.CsvToBeanFactory;
import csv.lab.LaboratoryData;
import csv.medcase.MedicalCaseData;
import csv.output.OutputData;
import csv.patient.PatientData;
import csv.procedure.ProcedureData;
import util.PreProcessingUtil;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

import static java.lang.System.getenv;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Objects.requireNonNull;

public class Main {

    public static void main(String[] args) throws IOException {
        var patientDataSource = requireNonNull(getenv("PATIENT_DATA_SOURCE"));
        var medCaseDataSource = requireNonNull(getenv("MED_CASE_DATA_SOURCE"));
        var procedureDataSource = requireNonNull(getenv("PROCEDURE_DATA_SOURCE"));
        var labDataSource = requireNonNull(getenv("LABORATORY_DATA_SOURCE"));
        var expectedResultsSource = requireNonNull(getenv("EXPECTED_RESULTS_SOURCE"));

        var referenceTimestamp = LocalDateTime.now();

        var patients = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(patientDataSource),
                PatientData.class).parse();
        var medCases = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(medCaseDataSource),
                MedicalCaseData.class).parse();
        var procedures = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(procedureDataSource),
                ProcedureData.class).parse();
        var labData = PreProcessingUtil.filterForNonNaNValues(CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(labDataSource),
                LaboratoryData.class).parse());
        var expectedResults = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(expectedResultsSource),
                OutputData.class).parse();

        var latestTimestamp = PreProcessingUtil.getLatestTimestamp(patients, labData, medCases, procedures);
        var secondsOffset = Math.abs(SECONDS.between(referenceTimestamp, latestTimestamp));

        var timeOffsetPatients = patients.stream()
                .map(p -> p.offsetTimesBySeconds(secondsOffset))
                .toList();
        var timeOffsetMedCases = medCases.stream()
                .map(mc -> mc.offsetTimesBySeconds(secondsOffset))
                .toList();
        var timeOffsetProcedures = procedures.stream()
                .map(p -> p.offsetTimesBySeconds(secondsOffset))
                .toList();
        var timeOffsetLabData = labData.stream()
                .map(ld -> ld.offsetTimesBySeconds(secondsOffset))
                .toList();

        var filteredProcedures = PreProcessingUtil.filterForProceduresNotOlderThan7Days(timeOffsetProcedures, referenceTimestamp);
        var filteredLabData = PreProcessingUtil.filterForLabValuesNotOlderThan7Days(timeOffsetLabData, referenceTimestamp);
    }
}
