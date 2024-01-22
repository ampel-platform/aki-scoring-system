package de.uni_leipzig.mds.feed.repo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import de.uni_leipzig.mds.feed.PatientCaseFeedDTO;
import de.uni_leipzig.mds.feed.repo.error.InsertionError;
import de.uni_leipzig.mds.feed.repo.error.LookupError;
import de.uni_leipzig.mds.feed.repo.error.ReplacementError;
import io.vavr.control.Either;

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Objects.requireNonNull;

public class PatientCaseFeedMongoDBRepository implements PatientCaseFeedRepository {

    private final MongoCollection<PatientCaseFeedDTO> collection;

    PatientCaseFeedMongoDBRepository(MongoCollection<PatientCaseFeedDTO> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }


    @Override
    public Either<InsertionError, PatientCaseFeedDTO> insertFeed(PatientCaseFeedDTO pcf) {
        requireNonNull(pcf, "patient case feed to be inserted must not be null");

        try {
            collection.insertOne(pcf);
            return Either.right(pcf);
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == DUPLICATE_KEY) {
                return Either.left(new InsertionError.PatientCaseAlreadyExists(pcf.getPatient().getId(),
                        pcf.getCaseId()));
            } else {
                return Either.left(new InsertionError.DBError(e.getCause()));
            }
        } catch (Exception e) {
            return Either.left(new InsertionError.DBError(e.getCause()));
        }
    }

    @Override
    public Either<LookupError, PatientCaseFeedDTO> findFeed(String patientId, String caseId) {
        requireNonNull(patientId, "patient id must not be null");
        if (patientId.isBlank()) {
            throw new IllegalArgumentException("patient id must not be blank");
        }

        requireNonNull(caseId, "case id must not be null");
        if (caseId.isBlank()) {
            throw new IllegalArgumentException("case id must not be null");
        }

        var patientCaseFeed = collection.find(
                        and(
                                eq("patient.patientId", patientId),
                                eq("caseId", caseId)
                        ))
                .first();

        if (patientCaseFeed == null) {
            return Either.left(new LookupError.PatientCaseNotFoundError(patientId, caseId));
        }

        return Either.right(patientCaseFeed);
    }

    @Override
    public Either<ReplacementError, PatientCaseFeedDTO> replaceFeed(String patientId, String caseId,
                                                                    int expectedCurrentVersion,
                                                                    PatientCaseFeedDTO updatedInstance) {
        requireNonNull(patientId, "patient id must not be null");
        if (patientId.isBlank()) {
            throw new IllegalArgumentException("patient id must not be blank");
        }

        requireNonNull(caseId, "case id must not be null");
        if (caseId.isBlank()) {
            throw new IllegalArgumentException("case id must not be null");
        }
        requireNonNull(updatedInstance, "updated instance must not be null");

        if (updatedInstance.getVersion() <= expectedCurrentVersion) {
            throw new IllegalArgumentException("version number has to increase");
        }

        if (collection.find(and(
                eq("patient.patientId", patientId),
                eq("caseId", caseId)
        )).first() == null) {
            return Either.left(new ReplacementError.PatientCaseNotFoundError(patientId, caseId));
        }
        if (collection.findOneAndReplace(and(
                eq("patient.patientId", patientId),
                eq("caseId", caseId),
                eq("version", expectedCurrentVersion)
        ), updatedInstance) == null) {
            return Either.left(new ReplacementError.PatientCaseVersionMismatchError(patientId, caseId,
                    expectedCurrentVersion));
        }
        return Either.right(updatedInstance);
    }
}
