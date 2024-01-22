package de.uni_leipzig.mds.feed.repo.error;

public sealed interface LookupError {
    record PatientCaseNotFoundError(String patientId, String caseId) implements LookupError {
    }
}
