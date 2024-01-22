package de.uni_leipzig.mds.feed.repo.error;

public sealed interface ReplacementError {
    record PatientCaseNotFoundError(String patientId, String caseId) implements ReplacementError {
    }

    record PatientCaseVersionMismatchError(String patientId, String caseId, int expectedVersion)
            implements ReplacementError {
    }
}
