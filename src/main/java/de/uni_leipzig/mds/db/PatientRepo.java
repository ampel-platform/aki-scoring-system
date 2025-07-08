package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.Patient;

import java.util.Optional;

public interface PatientRepo {

    Optional<Patient> findPatientById(String patientId);

    void upsertPatient(Patient patient);
}
