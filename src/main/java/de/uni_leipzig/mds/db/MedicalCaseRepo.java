package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;

import java.util.List;
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

        db.createCollection(MEDICAL_CASE_COLLECTION_NAME);
        var medCaseCollection = db.getCollection(MEDICAL_CASE_COLLECTION_NAME)
                .withDocumentClass(MedicalCase.class)
                .withWriteConcern(ACKNOWLEDGED);

        var uniquenessOpts = new IndexOptions().unique(true);
        medCaseCollection.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("caseId", new BsonInt32(1))
                )
        ), uniquenessOpts);

        return new MedicalCaseRepo(medCaseCollection);
    }

    Optional<MedicalCase> findById(String caseId) {
        requireNonNull(caseId, "case id must not be null");
        return Optional.ofNullable(
                collection.find(Filters.eq("caseId", caseId))
                        .first());
    }

    void upsertMedicalCase(MedicalCase medCase) {
        requireNonNull(medCase, "medical case must not be null");

//        var medCaseUpdates = Updates.combine(
//                Updates.set("patientIdRef", medCase.patientIdRef()),
//                Updates.set("creationTime", medCase.creationTime()),
//                Updates.set("cancellationTime", medCase.cancellationTime())
//        );
        var filter = Filters.eq("caseId", medCase.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true);

        collection.findOneAndReplace(filter, medCase, replaceOpts);
    }
}
