package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.TimeSeriesOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class LabValueRepo {
    private static final Logger LOGGER = LoggerFactory.getLogger(LabValueRepo.class);

    private static final String LABORATORY_COLLECTION_NAME = "lab";
    private static final int ENTRY_EXPIRATION_IN_DAYS = 8;

    private final MongoCollection<LabValue> collection;

    private LabValueRepo(MongoCollection<LabValue> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static LabValueRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        var labValueCollection = RepoUtil.getCollection(db, LABORATORY_COLLECTION_NAME, LabValue.class)
                .or(() -> createLabValueCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create lab value collection"));

        LOGGER.info("initialized lab value repo");
        return new LabValueRepo(labValueCollection);
    }

    private static Optional<MongoCollection<LabValue>> createLabValueCollection(MongoDatabase db) {
        var timeSeriesOpts = new TimeSeriesOptions("measuringTime")
                .metaField("caseId");

        var timeSeriesCollectionOpts = new CreateCollectionOptions()
                .timeSeriesOptions(timeSeriesOpts)
                .expireAfter(ENTRY_EXPIRATION_IN_DAYS, TimeUnit.DAYS);

        db.createCollection(LABORATORY_COLLECTION_NAME, timeSeriesCollectionOpts);
        return RepoUtil.getCollection(db, LABORATORY_COLLECTION_NAME, LabValue.class);
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
