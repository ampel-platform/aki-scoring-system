package de.uni_leipzig.mds.db;

import java.util.List;
import java.util.Optional;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class AkiRepo implements IAkiRepo {

    private final ProcedureRepo procedureRepo;
    private final LabValueRepo labValueRepo;
    private final MedicalCaseRepo medicalCaseRepo;
    private final PatientRepo patientRepo;

    public AkiRepo(ProcedureRepo procedureRepo, LabValueRepo labValueRepo, MedicalCaseRepo medicalCaseRepo,
                   PatientRepo patientRepo) {
        this.procedureRepo = requireNonNull(procedureRepo, "procedure repo must not be null");
        this.labValueRepo = requireNonNull(labValueRepo, "lab value repo must not be null");
        this.medicalCaseRepo = requireNonNull(medicalCaseRepo, "medical case repo must not be null");
        this.patientRepo = requireNonNull(patientRepo, "patient repo must not be null");
    }

    @Override
    public void insertProcedure(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");
        procedureRepo.insert(procedure);
    }

    @Override
    public void insertLabValue(LabValue labValue) {
        requireNonNull(labValue, "lab value must not be null");
        labValueRepo.insert(labValue);
    }

    @Override
    public void upsertMedicalCase(MedicalCase medicalCase) {
        requireNonNull(medicalCase, "medical case must not be null");
        medicalCaseRepo.upsertMedicalCase(medicalCase);
    }

    @Override
    public void upsertPatient(Patient patient) {
        requireNonNull(patient, "patient must not be null");
        patientRepo.upsertPatient(patient);
    }

    @Override
    public List<Procedure> findProceduresInCase(String caseId) {
        neitherNullNorBlank(caseId, "case id must neither be null nor blank");
        return procedureRepo.findAllByCase(caseId);
    }

    @Override
    public List<LabValue> findLabValuesInCase(String caseId) {
        neitherNullNorBlank(caseId, "case id must neither be null nor blank");
        return labValueRepo.findAllByCase(caseId);
    }

    @Override
    public Optional<Patient> findPatientInCase(String caseId) {
        neitherNullNorBlank(caseId, "case id must neither be null nor blank");
        return findMedicalCase(caseId)
                .flatMap(mc -> patientRepo.findById(mc.patientIdRef()));
    }

    @Override
    public Optional<MedicalCase> findMedicalCase(String caseId) {
        neitherNullNorBlank(caseId, "case id must neither be null nor blank");
        return medicalCaseRepo.findById(caseId);
    }
}
