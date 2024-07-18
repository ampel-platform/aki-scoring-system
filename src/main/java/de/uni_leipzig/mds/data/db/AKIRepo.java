package de.uni_leipzig.mds.data.db;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A repository for information/data related to an acute kidney injury (AKI).
 */
public final class AKIRepo implements IAKIRepo {

    private final LaboratoryValueRepo labValueRepo;

    private final ProcedureRepo procedureRepo;

    private final PatientRepo patientRepo;


    AKIRepo(LaboratoryValueRepo labValueRepo, ProcedureRepo procedureRepo, PatientRepo patientRepo) {
        this.labValueRepo = requireNonNull(labValueRepo, "laboratory value repository must not be null");
        this.procedureRepo = requireNonNull(procedureRepo, "procedure repository must not be null");
        this.patientRepo = requireNonNull(patientRepo, "patient repository must not be null");
    }

    public void insertPatient(Patient patient) {
        requireNonNull(patient, "patient must not be null");
        patientRepo.insert(patient);
    }

    public void insertLabValue(LaboratoryValue labValue) {
        requireNonNull(labValue, "lab value must not be null");
        labValueRepo.insert(labValue);
    }

    public void insertProcedure(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");
        procedureRepo.insert(procedure);
    }

    public Optional<PatientCaseFeed> findPatientFeed(String patientId, String caseId) {
        var patients = patientRepo.find(patientId);
        if (patients.isEmpty() || patients.getFirst() == null) {
            return Optional.empty();
        }
        var patient = patients.getFirst();

        var labValues = labValueRepo.findAllFor(patientId, caseId);
        var procedures = procedureRepo.findAllFor(patientId, caseId);

        return Optional.of(
                PatientCaseFeed.Builder.builder()
                        .dateOfBirth(patient.getDateOfBirth())
                        .deceased(patient.isDeceased())
                        .labValues(labValues)
                        .procedures(procedures)
                        .patientRef(patientId)
                        .caseRef(caseId)
                        .build());
    }
}
