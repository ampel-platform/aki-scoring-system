package de.uni_leipzig.mds.data.db;

import java.util.Optional;

/**
 * Describes a repository for information/data related to an acute kidney injury (AKI).
 */
public interface IAKIRepo {

    /**
     * Inserts a single {@link Patient} to this repository.
     *
     * @param patient patient to insert.
     */
    void insertPatient(Patient patient);

    /**
     * Inserts a single {@link LaboratoryValue} to this repository.
     *
     * @param labValue laboratory value to insert.
     */
    void insertLabValue(LaboratoryValue labValue);

    /**
     * Inserts a single {@link Procedure} to this repository.
     *
     * @param procedure procedure to insert.
     */
    void insertProcedure(Procedure procedure);

    /**
     * Tries to find AKI related information for a specific patient within a case. This information is bundled within a
     * {@link PatientCaseFeed} instance to resemble a timeline.
     *
     * @param patientId identifies the patient.
     * @param caseId    identifies the case.
     * @return A {@link PatientCaseFeed} if there is any.
     */
    Optional<PatientCaseFeed> findPatientFeed(String patientId, String caseId);
}
