package de.uni_leipzig.mds.db;

import java.util.List;
import java.util.Optional;

public interface IAkiRepo extends IProcedureRepo {

    void insertLabValue(LabValue labValue);

    void upsertMedicalCase(MedicalCase medicalCase);

    void upsertPatient(Patient patient);

    List<LabValue> findLabValuesInCase(String caseId);

    Optional<Patient> findPatientInCase(String caseId);

    Optional<MedicalCase> findMedicalCase(String caseId);
}
