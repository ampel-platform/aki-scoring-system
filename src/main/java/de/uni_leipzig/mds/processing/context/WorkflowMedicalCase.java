package de.uni_leipzig.mds.processing.context;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record WorkflowMedicalCase(
        String caseId,
        Instant creationDate,
        Instant cancellationDate,
        WorkflowPatient patient,
        List<WorkflowLabValue> creatinineValues,
        List<WorkflowLabValue> vancomycinValues,
        List<WorkflowProcedure> procedures
) {
    // TODO: add constructor for enforcing an existing caseId
    public WorkflowMedicalCase filterForValuesBetween(Instant from, Instant to) {
        var timeFilteredCreatinineLabValues = creatinineValues.stream().filter(
                        cv -> cv.measuringTime().compareTo(from) >= 0 // >= from
                                && cv.measuringTime().compareTo(to) <= 0) // <= to
                .toList();

        var timeFilteredVancomycinLabValues = vancomycinValues.stream().filter(
                        vv -> vv.measuringTime().compareTo(from) >= 0 // >= from
                                && vv.measuringTime().compareTo(to) <= 0) // <= to
                .toList();

        var timeFilteredProcedures = procedures.stream().filter(
                        p -> p.conductTime().compareTo(from) >= 0 // >= from
                                && p.conductTime().compareTo(to) <= 0) // <= to
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
