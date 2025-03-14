package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.TimeSeriesOptions;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;

public final class LabValueRepo {

    private static final String LABORATORY_COLLECTION_NAME = "lab";
    private static final int ENTRY_EXPIRATION_IN_DAYS = 8;

    private final MongoCollection<LabValue> collection;

    private LabValueRepo(MongoCollection<LabValue> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static LabValueRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        var timeSeriesOpts = new TimeSeriesOptions("measuringTime")
                .metaField("caseId");

        var timeSeriesCollectionOpts = new CreateCollectionOptions()
                .timeSeriesOptions(timeSeriesOpts)
                .expireAfter(ENTRY_EXPIRATION_IN_DAYS, TimeUnit.DAYS);

        db.createCollection(LABORATORY_COLLECTION_NAME, timeSeriesCollectionOpts);
        var labValueCollection = db.getCollection(LABORATORY_COLLECTION_NAME)
                .withDocumentClass(LabValue.class)
                .withWriteConcern(ACKNOWLEDGED);

        var uniquenessOpts = new IndexOptions().unique(true);
        labValueCollection.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("caseId", new BsonInt32(1)),
                        new BsonElement("orderId", new BsonInt32(1)),
                        new BsonElement("analyte", new BsonInt32(1))
                )
        ), uniquenessOpts);

        return new LabValueRepo(labValueCollection);
    }

    List<LabValue> findAllByCase(String caseId) {
        requireNonNull(caseId, "case id must not be null");

        var labValues = collection.find(Filters.eq("caseId", caseId));
        return StreamSupport.stream(labValues.spliterator(), false).toList();
    }

    void insert(LabValue labValue) {
        requireNonNull(labValue, "lab value must not be null");
        collection.insertOne(labValue);
    }
}
