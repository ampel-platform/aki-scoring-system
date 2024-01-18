package de.uni_leipzig.mds.feed;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode
public final class PatientCaseFeed {

    @Getter
    private final ObjectId id;
    @Getter
    private final String caseId;
    private final List<LaboratoryValue> creatinineValues;
    private final List<LaboratoryValue> vancomycinValues;
    private final List<Procedure> dialysisProcedures;
    @Getter
    private final Patient patient;
    @Getter
    private final int version;

    private PatientCaseFeed(ObjectId id, String caseId, List<LaboratoryValue> creatinineValues,
                            List<LaboratoryValue> vancomycinValues, List<Procedure> dialysisProcedures,
                            Patient patient, int version) {
        this.id = id;
        this.caseId = caseId;
        this.creatinineValues = creatinineValues;
        this.vancomycinValues = vancomycinValues;
        this.dialysisProcedures = dialysisProcedures;
        this.patient = patient;
        this.version = version;
    }

    PatientCaseFeed(PatientCaseFeedDTO dto) {
        this.id = dto.getId();
        this.caseId = dto.getCaseId();
        this.creatinineValues = dto.getCreatinineValues().stream()
                .map(LaboratoryValue::new)
                .toList();
        this.vancomycinValues = dto.getVancomycinValues().stream()
                .map(LaboratoryValue::new)
                .toList();
        this.dialysisProcedures = dto.getDialysisProcedures().stream()
                .map(Procedure::new)
                .toList();
        this.patient = new Patient(dto.getPatient());
        this.version = dto.getVersion();
    }

    PatientCaseFeed(ObjectId id, Patient patient, String caseId) {
        this.id = requireNonNull(id, "id must not be null");
        this.patient = requireNonNull(patient, "patient must not be null");
        this.caseId = requireNonNull(caseId, "case id must not be null");
        this.creatinineValues = List.of();
        this.vancomycinValues = List.of();
        this.dialysisProcedures = List.of();
        this.version = 1;
    }

    public PatientCaseFeed markPatientDeceased() {
        return new PatientCaseFeed(id, caseId, List.copyOf(creatinineValues), List.copyOf(vancomycinValues),
                List.copyOf(dialysisProcedures), new Patient(patient.getId(), patient.getDateOfBirth(), true),
                (version + 1));
    }

    public PatientCaseFeed addCreatinineValue(LocalDateTime time, double value, String unit) {
        requireNonNull(time, "time must not be null");
        if (time.isBefore(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("creatinine value cannot be measured before patient's date of birth");
        }

        if (value < 0) {
            throw new IllegalArgumentException("creatinine value cannot be negative");
        }

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit for creatinine values must not be blank");
        }

        var updatedCreatinineValues = new ArrayList<>(creatinineValues);
        updatedCreatinineValues.add(new LaboratoryValue(time, value, unit));

        return new PatientCaseFeed(id, caseId, List.copyOf(updatedCreatinineValues), List.copyOf(vancomycinValues),
                List.copyOf(dialysisProcedures), patient, (version + 1));
    }

    public PatientCaseFeed addVancomycinValue(LocalDateTime time, double value, String unit) {
        requireNonNull(time, "time must not be null");
        if (time.isBefore(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("vancomycin value cannot be measured before patient's date of birth");
        }

        if (value < 0) {
            throw new IllegalArgumentException("vancomycin value cannot be negative");
        }

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit for vancomycin values must not be blank");
        }

        var updatedVancomycinValues = new ArrayList<>(vancomycinValues);
        updatedVancomycinValues.add(new LaboratoryValue(time, value, unit));

        return new PatientCaseFeed(id, caseId, List.copyOf(creatinineValues), List.copyOf(updatedVancomycinValues),
                List.copyOf(dialysisProcedures), patient, (version + 1));
    }

    public PatientCaseFeed addDialysisProcedure(LocalDateTime time, String code) {
        requireNonNull(time, "time must not be null");
        if (time.isBefore(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("dialysis procedure cannot be conducted before patient's date of birth");
        }

        requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code for dialysis procedure must not be blank");
        }

        var updatedDialysisProcedures = new ArrayList<>(dialysisProcedures);
        updatedDialysisProcedures.add(new Procedure(time, code));

        return new PatientCaseFeed(id, caseId, List.copyOf(creatinineValues), List.copyOf(vancomycinValues),
                List.copyOf(updatedDialysisProcedures), patient, (version + 1));
    }

    public List<LaboratoryValue> getCreatinineValues() {
        return List.copyOf(creatinineValues);
    }

    public List<LaboratoryValue> getVancomycinValues() {
        return List.copyOf(vancomycinValues);
    }

    public List<Procedure> getDialysisProcedures() {
        return List.copyOf(dialysisProcedures);
    }
}
