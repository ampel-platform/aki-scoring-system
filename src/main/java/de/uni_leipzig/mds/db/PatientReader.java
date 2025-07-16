package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.Patient;

import java.util.Optional;

public interface PatientReader {

    Optional<Patient> findLatestPatientById(String patientId);

}
