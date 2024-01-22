package de.uni_leipzig.mds.feed.repo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import de.uni_leipzig.mds.feed.LaboratoryValueDTO;
import de.uni_leipzig.mds.feed.PatientCaseFeedDTO;
import de.uni_leipzig.mds.feed.PatientDTO;
import de.uni_leipzig.mds.feed.ProcedureDTO;
import org.bson.BsonDocument;
import org.bson.BsonElement;
import org.bson.BsonInt32;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class PatientCaseFeedRepositoryFactory {

    private PatientCaseFeedRepositoryFactory() {
    }

    public static PatientCaseFeedRepository newMongoDBRepository(MongoDBConnectionSettings connSettings,
                                                                 String applicationName, String repoName) {

        requireNonNull(connSettings, "connections settings must not be null");
        requireNonNull(applicationName, "application name must not be null");
        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("application name must not be blank");
        }
        requireNonNull(repoName, "repository name must not be null");
        if (repoName.isBlank()) {
            throw new IllegalArgumentException("repository name must not be blank");
        }

        var connString = new ConnectionString("mongodb://%s:%s".formatted(connSettings.host(), connSettings.port()));
        var mongoDBClient = createMongoDBClient(connString, applicationName);
        var db = mongoDBClient.getDatabase(connSettings.dbName());
        var collection = getMongoDBCollection(db, repoName);
        setUpMongoDBDocumentUniqueness(collection);

        return new PatientCaseFeedMongoDBRepository(collection);
    }

    private static MongoClient createMongoDBClient(ConnectionString connectionString, String applicationName) {
        var clientSettings = MongoClientSettings.builder()
                .codecRegistry(createMongoDBCodecRegistry())
                .applyConnectionString(connectionString)
                .applicationName(applicationName)
                .build();

        return MongoClients.create(clientSettings);
    }

    private static CodecRegistry createMongoDBCodecRegistry() {
        var pojoCodecProvider = PojoCodecProvider.builder()
                .register(PatientCaseFeedDTO.class, LaboratoryValueDTO.class, PatientDTO.class, ProcedureDTO.class)
                .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
                .build();
        var pojoCodecRegistry = fromProviders(pojoCodecProvider);
        return fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    }

    private static MongoCollection<PatientCaseFeedDTO> getMongoDBCollection(MongoDatabase db, String collectionName) {
        return db.getCollection(collectionName, PatientCaseFeedDTO.class)
                .withWriteConcern(ACKNOWLEDGED);
    }

    private static void setUpMongoDBDocumentUniqueness(MongoCollection<PatientCaseFeedDTO> coll) {
        var uniquenessOptions = new IndexOptions().unique(true);
        coll.createIndex(new BsonDocument(
                List.of(
                        new BsonElement("patient.patient_id", new BsonInt32(1)),
                        new BsonElement("case_id", new BsonInt32(1))
                )
        ), uniquenessOptions);
    }

    public record MongoDBConnectionSettings(String host, String port, String dbName) {
    }
}
