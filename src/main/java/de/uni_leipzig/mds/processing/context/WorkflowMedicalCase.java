package de.uni_leipzig.mds.processing.context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record WorkflowMedicalCase(
        String caseId,
        LocalDate creationDate,
        LocalDate cancellationDate,
        WorkflowPatient patient,
        List<WorkflowLabValue> creatinineValues,
        List<WorkflowLabValue> vancomycinValues,
        List<WorkflowProcedure> procedures
) {
    // TODO: add constructor for enforcing an existing caseId
    public WorkflowMedicalCase filterForValuesBetween(LocalDateTime from, LocalDateTime to) {
        var timeFilteredCreatinineLabValues = creatinineValues.stream().filter(
                        cv -> (cv.measuringTime().isEqual(from) || cv.measuringTime().isAfter(from))
                                && (cv.measuringTime().isEqual(to) || cv.measuringTime().isBefore(to)))
                .toList();

        var timeFilteredVancomycinLabValues = creatinineValues.stream().filter(
                        vv -> (vv.measuringTime().isEqual(from) || vv.measuringTime().isAfter(from))
                                && (vv.measuringTime().isEqual(to) || vv.measuringTime().isBefore(to)))
                .toList();

        var timeFilteredProcedures = procedures.stream().filter(
                        p -> (p.conductTime().isEqual(from) || p.conductTime().isAfter(from))
                                && (p.conductTime().isEqual(to) || p.conductTime().isBefore(to)))
                .toList();

        return new WorkflowMedicalCase(
                caseId,
                creationDate,
                cancellationDate,
                patient,
                timeFilteredCreatinineLabValues,
                timeFilteredVancomycinLabValues,
                timeFilteredProcedures
        );
    }

    public boolean isSufficientForWorkflowRun() {
        if (patient == null) {
            return false;
        }

        if (patient.id() == null || patient.id().isBlank() || patient.dateOfBirth() == null) {
            return false;
        }

        return !creatinineValues.isEmpty();
    }

    public List<WorkflowLabValue> getCreatinineValuesDesc() {
        return creatinineValues.stream().sorted(Comparator.comparing(WorkflowLabValue::measuringTime))
                .toList()
                .reversed();
    }

    public Optional<WorkflowLabValue> getLatestCreatinineValue() {
        var creatinineValuesDescSorted = getCreatinineValuesDesc();

        if (creatinineValuesDescSorted.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(creatinineValuesDescSorted.getFirst());
        }
    }
}
