package de.uni_leipzig.mds.data.db;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;

/**
 * Factory for creating persistence related repositories.
 */
public final class RepoFactory {

    private final MongoDatabase db;

    public RepoFactory(MongoDatabase db) {
        this.db = requireNonNull(db, "db must not be null");
    }

    /**
     * Creates a {@link AKIRepo} instance.
     *
     * @return The {@link AKIRepo}.
     */
    public AKIRepo createAkiRepo() {
        return new AKIRepo(
                createLaboratoryValueRepo(),
                createProcedureRepo(),
                createPatientRepo()
        );
    }

    private LaboratoryValueRepo createLaboratoryValueRepo() {
        db.createCollection(LaboratoryValueRepo.COLLECTION_NAME);
        var coll = db.getCollection(LaboratoryValueRepo.COLLECTION_NAME, LaboratoryValue.class)
                .withWriteConcern(ACKNOWLEDGED);

        var uniquenessOptions = new IndexOptions().unique(true);
        coll.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("patientIdRef", new BsonInt32(1)),
                        new BsonElement("caseIdRef", new BsonInt32(1)),
                        new BsonElement("orderIdRef", new BsonInt32(1)),
                        new BsonElement("analyteCode", new BsonInt32(1))
                )
        ), uniquenessOptions);

        var expirationOptions = new IndexOptions().expireAfter(8L, TimeUnit.DAYS);
        coll.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("measurementTime", new BsonInt32(1))
                )
        ), expirationOptions);

        return new LaboratoryValueRepo(coll);
    }

    private ProcedureRepo createProcedureRepo() {
        db.createCollection(ProcedureRepo.COLLECTION_NAME);
        var coll = db.getCollection(ProcedureRepo.COLLECTION_NAME, Procedure.class)
                .withWriteConcern(ACKNOWLEDGED);

        var expirationOptions = new IndexOptions().expireAfter(8L, TimeUnit.DAYS);
        coll.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("conductTime", new BsonInt32(1))
                )
        ), expirationOptions);

        // TODO: setup unique index for surgical procedure code sequence number
        return new ProcedureRepo(coll);
    }

    private PatientRepo createPatientRepo() {
        db.createCollection(PatientRepo.COLLECTION_NAME);
        var coll = db.getCollection(PatientRepo.COLLECTION_NAME, Patient.class)
                .withWriteConcern(ACKNOWLEDGED);

        var uniquenessOptions = new IndexOptions().unique(true);
        coll.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("patientId", new BsonInt32(1))
                )
        ), uniquenessOptions);

        return new PatientRepo(coll);
    }
}
