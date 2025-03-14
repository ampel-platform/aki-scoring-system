package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.TimeSeriesOptions;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;

public final class ProcedureRepo {

    private static final String PROCEDURE_COLLECTION_NAME = "procedure";
    private static final int ENTRY_EXPIRATION_IN_DAYS = 8;

    private final MongoCollection<Procedure> collection;

    private ProcedureRepo(MongoCollection<Procedure> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static ProcedureRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        var timeSeriesOpts = new TimeSeriesOptions("conductTime")
                .metaField("caseId");

        var timeSeriesCollectionOpts = new CreateCollectionOptions()
                .timeSeriesOptions(timeSeriesOpts)
                .expireAfter(ENTRY_EXPIRATION_IN_DAYS, TimeUnit.DAYS);

        db.createCollection(PROCEDURE_COLLECTION_NAME, timeSeriesCollectionOpts);
        return new ProcedureRepo(db.getCollection(PROCEDURE_COLLECTION_NAME)
                .withDocumentClass(Procedure.class)
                .withWriteConcern(ACKNOWLEDGED));
    }

    List<Procedure> findAllByCase(String caseId) {
        requireNonNull(caseId, "case id must not be null");

        var procedures = collection.find(Filters.eq("caseId", caseId));
        return StreamSupport.stream(procedures.spliterator(), false).toList();
    }

    void insert(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");
        collection.insertOne(procedure);
    }
}
