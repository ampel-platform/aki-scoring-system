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
import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public class PatientRepo {

    private static final String PATIENT_COLLECTION_NAME = "patient";

    private final MongoCollection<Patient> collection;

    private PatientRepo(MongoCollection<Patient> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static PatientRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        db.createCollection(PATIENT_COLLECTION_NAME);
        var patientCollection = db.getCollection(PATIENT_COLLECTION_NAME)
                .withDocumentClass(Patient.class)
                .withWriteConcern(ACKNOWLEDGED);

        var uniquenessOpts = new IndexOptions().unique(true);
        patientCollection.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("patientId", new BsonInt32(1))
                )
        ), uniquenessOpts);

        return new PatientRepo(patientCollection);
    }

    Optional<Patient> findById(String patientId) {
        neitherNullNorBlank(patientId, "patient id must neither be null nor blank");

        return Optional.ofNullable(
                collection.find(Filters.eq("patientId", patientId))
                        .first());
    }

    void upsertPatient(Patient patient) {
        requireNonNull(patient, "patient must not be null");

//        var patientUpdates = Updates.combine(
//                Updates.set("dateOfBirth", patient.dateOfBirth()),
//                Updates.set("deceased", patient.deceased()),
//                Updates.set("dischargeTime", patient.dischargeTime())
//        );
        var filter = Filters.eq("patientId", patient.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true);

        collection.findOneAndReplace(filter, patient, replaceOpts);
    }
}
