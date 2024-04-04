package de.uni_leipzig.mds.data.db;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static de.uni_leipzig.mds.data.db.Codes.LAB_CODES_CREATININE;
import static de.uni_leipzig.mds.data.db.Codes.LAB_CODES_VANCOMYCIN;
import static java.util.Objects.requireNonNull;

/**
 * Describes a historical feed (aka a timeline) of information related to an acute kidney injury (AKI) for a single
 * patient.
 */
@Getter
@EqualsAndHashCode
public final class PatientCaseFeed {
    private final String patientId;
    private final String caseId;
    private final LocalDateTime dateOfBirth;
    private final boolean deceased;
    private final List<LaboratoryValue> creatinineValues; // Values in DESC order
    private final List<LaboratoryValue> vancomycinValues; // Values in DESC order
    private final List<Procedure> dialysisProcedures; // Values in DESC order

    private PatientCaseFeed(String patientId, String caseId,
                            LocalDateTime dateOfBirth,
                            boolean deceased,
                            List<LaboratoryValue> creatinineValues,
                            List<LaboratoryValue> vancomycinValues,
                            List<Procedure> dialysisProcedures) {
        this.patientId = requireNonNull(patientId, "patient ID must not be null");
        this.caseId = requireNonNull(caseId, "case ID must not be null");
        this.dateOfBirth = requireNonNull(dateOfBirth, "date of birth must not be null");
        this.deceased = deceased;

        requireNonNull(creatinineValues, "creatinine values must not be null");
        this.creatinineValues = creatinineValues.stream()
                .sorted(Comparator.comparing(LaboratoryValue::getTime)
                        .reversed())
                .toList();

        requireNonNull(vancomycinValues, "vancomycin values must not be null");
        this.vancomycinValues = vancomycinValues.stream()
                .sorted(Comparator.comparing(LaboratoryValue::getTime)
                        .reversed())
                .toList();

        requireNonNull(dialysisProcedures, "dialysis procedures must not be null");
        this.dialysisProcedures = dialysisProcedures.stream()
                .sorted(Comparator.comparing(Procedure::getTime)
                        .reversed())
                .toList();
    }


    @FunctionalInterface
    interface RequireDateOfBirth<T> {
        T dateOfBirth(LocalDateTime dateOfBirth);
    }

    @FunctionalInterface
    interface RequireVitalStatus<T> {
        T deceased(boolean deceased);
    }

    @FunctionalInterface
    interface RequireLabValues<T> {
        T labValues(List<LaboratoryValue> laboratoryValues);
    }

    @FunctionalInterface
    interface RequireProcedures<T> {
        T procedures(List<Procedure> procedures);
    }

    @FunctionalInterface
    interface RequirePatientRef<T> {
        T patientRef(String patientId);
    }

    @FunctionalInterface
    interface RequireCaseRef<T> {
        T caseRef(String caseId);
    }

    @FunctionalInterface
    interface RequireRefs<T> extends RequirePatientRef<RequireCaseRef<T>> {
        default T refs(String patientId, String caseId) {
            return patientRef(patientId)
                    .caseRef(caseId);
        }
    }

    static class Builder {
        private Builder() {
        }

        @AllArgsConstructor
        static class BuildStage {
            private String patientId;
            private String caseId;
            private LocalDateTime dateOfBirth;
            private boolean deceased;
            private List<LaboratoryValue> labValues;
            private List<Procedure> procedures;

            public PatientCaseFeed build() {
                var creatinineValues = labValues.stream()
                        .filter(val -> LAB_CODES_CREATININE.contains(val.getAnalyteCode().toUpperCase())).toList();

                var vancomycinValues = labValues.stream()
                        .filter(val -> LAB_CODES_VANCOMYCIN.contains(val.getAnalyteCode().toUpperCase())).toList();

                return new PatientCaseFeed(
                        patientId,
                        caseId,
                        dateOfBirth,
                        deceased,
                        creatinineValues,
                        vancomycinValues,
                        procedures
                );
            }
        }

        public static RequireDateOfBirth<RequireVitalStatus<RequireLabValues<RequireProcedures<RequireRefs<BuildStage>>>>>
        builder() {
            return dateOfBirth -> deceased -> labValues -> procedures -> patientId -> caseId -> new BuildStage(
                    patientId, caseId, dateOfBirth, deceased, labValues, procedures
            );
        }
    }
}
