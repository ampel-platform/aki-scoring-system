package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.MedicalCase;

import java.util.Optional;

public interface MedicalCaseRepo {

    Optional<MedicalCase> findMedicalCaseById(String medicalCaseId);

    void upsertMedicalCase(MedicalCase medicalCase);
}
