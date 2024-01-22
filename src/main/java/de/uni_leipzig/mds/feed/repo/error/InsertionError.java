package de.uni_leipzig.mds.feed.repo.error;

public sealed interface InsertionError {
    record PatientCaseAlreadyExists(String patientId, String caseId) implements InsertionError {
    }

    record DBError(Throwable cause) implements InsertionError {
    }
}
