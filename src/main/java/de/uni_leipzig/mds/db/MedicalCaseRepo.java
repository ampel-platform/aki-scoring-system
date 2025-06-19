package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;

import java.util.Optional;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;

public final class MedicalCaseRepo {
    private static final String MEDICAL_CASE_COLLECTION_NAME = "medcase";

    private final MongoCollection<MedicalCase> collection;

    private MedicalCaseRepo(MongoCollection<MedicalCase> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static MedicalCaseRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        var medicalCaseCollection = RepoUtil.getCollection(db, MEDICAL_CASE_COLLECTION_NAME, MedicalCase.class)
                .or(() -> createMedicalCaseCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create medical case collection"));

        return new MedicalCaseRepo(medicalCaseCollection);
    }

    private static Optional<MongoCollection<MedicalCase>> createMedicalCaseCollection(MongoDatabase db) {
        db.createCollection(MEDICAL_CASE_COLLECTION_NAME);

        return Optional.of(
                db.getCollection(MEDICAL_CASE_COLLECTION_NAME)
                        .withDocumentClass(MedicalCase.class)
                        .withWriteConcern(ACKNOWLEDGED));
    }

    Optional<MedicalCase> findById(String caseId) {
        requireNonNull(caseId, "case id must not be null");
        return Optional.ofNullable(
                collection.find(Filters.eq("_id", caseId))
                        .first());
    }

    void upsertMedicalCase(MedicalCase medCase) {
        requireNonNull(medCase, "medical case must not be null");

        var filter = Filters.eq("_id", medCase.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true);

        collection.findOneAndReplace(filter, medCase, replaceOpts);
    }
}
