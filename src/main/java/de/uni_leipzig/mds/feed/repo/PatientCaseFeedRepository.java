package de.uni_leipzig.mds.feed.repo;

import de.uni_leipzig.mds.feed.PatientCaseFeedDTO;
import de.uni_leipzig.mds.feed.repo.error.InsertionError;
import de.uni_leipzig.mds.feed.repo.error.LookupError;
import de.uni_leipzig.mds.feed.repo.error.ReplacementError;
import io.vavr.control.Either;

public interface PatientCaseFeedRepository {

    Either<InsertionError, PatientCaseFeedDTO> insertFeed(PatientCaseFeedDTO pcf);

    Either<LookupError, PatientCaseFeedDTO> findFeed(String patientId, String caseId);

    Either<ReplacementError, PatientCaseFeedDTO> replaceFeed(String patientId, String caseId, int expectedCurrentVersion,
                                                             PatientCaseFeedDTO update);
}
