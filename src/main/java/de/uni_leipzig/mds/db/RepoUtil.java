package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;

public final class RepoUtil {
    private RepoUtil() {
    }

    static <T> Optional<MongoCollection<T>> getCollection(MongoDatabase db, String collectionName, Class<T> clazz) {
        var collectionExists = StreamSupport.stream(db.listCollectionNames().spliterator(), false)
                .anyMatch(name -> name.equalsIgnoreCase(collectionName));

        if (collectionExists) {
            return Optional.of(db.getCollection(collectionName)
                    .withDocumentClass(clazz)
                    .withWriteConcern(ACKNOWLEDGED));
        } else {
            return Optional.empty();
        }
    }
}
