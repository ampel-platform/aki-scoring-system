package de.uni_leipzig.mds.db;

/**
 * Marker interface acting as an intersection type for various AKI related repositories.
 */
public interface AkiRepo extends LaboratoryRepo, MedicalCaseRepo, PatientRepo, ProcedureRepo {
}
