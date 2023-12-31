package de.uni_leipzig.mds.feed;

import lombok.Getter;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class PatientCaseFeed {

    private final ObjectId id;

    @Getter
    private final String caseId;
    private final List<LaboratoryValue> creatinineValues;
    private final List<LaboratoryValue> vancomycinValues;
    private final List<Procedure> dialysisProcedures;

    @Getter
    private Patient patient;

    @Getter
    private int version;

    PatientCaseFeed(PatientCaseFeedDTO dto) {
        this.id = dto.getId();
        this.caseId = dto.getCaseId();
        this.creatinineValues = dto.getCreatinineValues().stream()
                .map(cv -> new LaboratoryValue(
                        cv.getTime(),
                        cv.getValue(),
                        cv.getUnit()))
                .collect(Collectors.toCollection(ArrayList::new));
        this.vancomycinValues = dto.getVancomycinValues().stream()
                .map(vv -> new LaboratoryValue(
                        vv.getTime(),
                        vv.getValue(),
                        vv.getUnit()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        this.dialysisProcedures = dto.getDialysisProcedures().stream()
                .map(dp -> new Procedure(
                        dp.getTime(),
                        dp.getCode()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        this.patient = new Patient(
                dto.getPatient().getId(),
                dto.getPatient().getDateOfBirth(),
                dto.getPatient().getDeceased());
        this.version = dto.getVersion();
    }

    PatientCaseFeed(ObjectId id, Patient patient, String caseId) {
        this.id = requireNonNull(id, "id must not be null");
        this.patient = requireNonNull(patient, "patient must not be null");
        this.caseId = requireNonNull(caseId, "case id must not be null");
        this.creatinineValues = new ArrayList<>();
        this.vancomycinValues = new ArrayList<>();
        this.dialysisProcedures = new ArrayList<>();
        this.version = 1;
    }

    public void markPatientDeceased() {
        patient = new Patient(patient.getId(), patient.getDateOfBirth(), true);
        version++;
    }

    public void addCreatinineValue(Date time, double value, String unit) {
        requireNonNull(time, "time must not be null");
        if (time.before(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("creatinine value cannot be measured before patient's date of birth");
        }

        if (value < 0) {
            throw new IllegalArgumentException("creatinine value cannot be negative");
        }

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit for creatinine values must not be blank");
        }

        creatinineValues.add(new LaboratoryValue(time, value, unit));
        version++;
    }

    public void addVancomycinValue(Date time, double value, String unit) {
        requireNonNull(time, "time must not be null");
        if (time.before(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("vancomycin value cannot be measured before patient's date of birth");
        }

        if (value < 0) {
            throw new IllegalArgumentException("vancomycin value cannot be negative");
        }

        requireNonNull(unit, "unit must not be null");
        if (unit.isBlank()) {
            throw new IllegalArgumentException("unit for vancomycin values must not be blank");
        }

        vancomycinValues.add(new LaboratoryValue(time, value, unit));
        version++;
    }

    public void addDialysisProcedure(Date time, String code) {
        requireNonNull(time, "time must not be null");
        if (time.before(patient.getDateOfBirth())) {
            throw new IllegalArgumentException("dialysis procedure cannot be conducted before patient's date of birth");
        }

        requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code for dialysis procedure must not be blank");
        }

        dialysisProcedures.add(new Procedure(time, code));
        version++;
    }

    public ObjectId getId() {
        return new ObjectId(id.toHexString());
    }

    public List<LaboratoryValue> getCreatinineValues() {
        return creatinineValues.stream()
                .map(LaboratoryValue::new)
                .toList();
    }

    public List<LaboratoryValue> getVancomycinValues() {
        return vancomycinValues.stream()
                .map(LaboratoryValue::new)
                .toList();
    }

    public List<Procedure> getDialysisProcedures() {
        return dialysisProcedures.stream()
                .map(Procedure::new)
                .toList();
    }
}
