package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.MedicalCase;

import java.util.Optional;

public interface MedicalCaseReader {
    Optional<MedicalCase> findLatestMedicalCaseById(String medicalCaseId);
}
