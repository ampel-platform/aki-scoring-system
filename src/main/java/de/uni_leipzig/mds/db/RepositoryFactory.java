package de.uni_leipzig.mds.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.TimeSeriesOptions;
import de.uni_leipzig.mds.db.types.LaboratoryEntry;
import de.uni_leipzig.mds.db.types.MedicalCase;
import de.uni_leipzig.mds.db.types.Patient;
import de.uni_leipzig.mds.db.types.Procedure;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static java.util.Objects.requireNonNull;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public final class RepositoryFactory {

    private static final String LABORATORY_COLLECTION_NAME = "laboratory";
    private static final String MEDICAL_CASE_COLLECTION_NAME = "medicalcase";
    private static final String PATIENT_COLLECTION_NAME = "patient";
    private static final String PROCEDURE_COLLECTION_NAME = "procedure";

    private RepositoryFactory() {
    }

    public static AkiRepo getInitializedAkiRepo(ConnectionString dbConnectionString) {
        requireNonNull(dbConnectionString, "db connection string must not be null");
        if (dbConnectionString.getDatabase() == null) {
            throw new IllegalArgumentException("db connection string must include the database name");
        }

        var dbClient = RepositoryFactoryUtil.createDBClient(dbConnectionString);
        var db = dbClient.getDatabase(dbConnectionString.getDatabase());

        return new AkiRepoImpl(
                getInitializedLaboratoryCollection(db),
                getInitializedMedicalCaseCollection(db),
                getInitializedPatientCollection(db),
                getInitializedProcedureCollection(db));
    }

    // Laboratory ------------------------------------------------------------------------------------------------------
    private static MongoCollection<LaboratoryEntry> getInitializedLaboratoryCollection(MongoDatabase db) {
        return RepositoryFactoryUtil.getCollection(db, LABORATORY_COLLECTION_NAME, LaboratoryEntry.class)
                .or(() -> createLabValueCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create lab value collection"));
    }

    private static Optional<MongoCollection<LaboratoryEntry>> createLabValueCollection(MongoDatabase db) {
        var timeSeriesOpts = new TimeSeriesOptions("measuringTime")
                .metaField("medicalCaseId");

        var timeSeriesCollectionOpts = new CreateCollectionOptions()
                .timeSeriesOptions(timeSeriesOpts);

        db.createCollection(LABORATORY_COLLECTION_NAME, timeSeriesCollectionOpts);
        return RepositoryFactoryUtil.getCollection(db, LABORATORY_COLLECTION_NAME, LaboratoryEntry.class);
    }

    // Medical Case ----------------------------------------------------------------------------------------------------
    private static MongoCollection<MedicalCase> getInitializedMedicalCaseCollection(MongoDatabase db) {
        return RepositoryFactoryUtil.getCollection(db, MEDICAL_CASE_COLLECTION_NAME, MedicalCase.class)
                .or(() -> createMedicalCaseCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create medical case collection"));
    }

    private static Optional<MongoCollection<MedicalCase>> createMedicalCaseCollection(MongoDatabase db) {
        db.createCollection(MEDICAL_CASE_COLLECTION_NAME);
        db.getCollection(MEDICAL_CASE_COLLECTION_NAME)
                .createIndex(Indexes.text("medicalCaseId"));
        return RepositoryFactoryUtil.getCollection(db, MEDICAL_CASE_COLLECTION_NAME, MedicalCase.class);
    }

    // Patient ---------------------------------------------------------------------------------------------------------
    private static MongoCollection<Patient> getInitializedPatientCollection(MongoDatabase db) {
        return RepositoryFactoryUtil.getCollection(db, PATIENT_COLLECTION_NAME, Patient.class)
                .or(() -> createPatientCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create patient collection"));
    }

    private static Optional<MongoCollection<Patient>> createPatientCollection(MongoDatabase db) {
        db.createCollection(PATIENT_COLLECTION_NAME);
        db.getCollection(PATIENT_COLLECTION_NAME)
                .createIndex(Indexes.text("patientId"));
        return RepositoryFactoryUtil.getCollection(db, PATIENT_COLLECTION_NAME, Patient.class);
    }

    // Procedure -------------------------------------------------------------------------------------------------------
    private static MongoCollection<Procedure> getInitializedProcedureCollection(MongoDatabase db) {
        return RepositoryFactoryUtil.getCollection(db, PROCEDURE_COLLECTION_NAME, Procedure.class)
                .or(() -> createProcedureCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create procedure collection"));
    }

    private static Optional<MongoCollection<Procedure>> createProcedureCollection(MongoDatabase db) {
        var timeSeriesOpts = new TimeSeriesOptions("conductTime")
                .metaField("medicalCaseId");

        var timeSeriesCollectionOpts = new CreateCollectionOptions()
                .timeSeriesOptions(timeSeriesOpts);

        db.createCollection(PROCEDURE_COLLECTION_NAME, timeSeriesCollectionOpts);
        return RepositoryFactoryUtil.getCollection(db, PROCEDURE_COLLECTION_NAME, Procedure.class);
    }


    // Utilities
    private static class RepositoryFactoryUtil {
        private static MongoClient createDBClient(ConnectionString connString) {
            var clientSettings = MongoClientSettings.builder()
                    .codecRegistry(createMongoDBCodecRegistry())
                    .applyConnectionString(connString)
                    .build();

            return MongoClients.create(clientSettings);
        }

        private static CodecRegistry createMongoDBCodecRegistry() {
            var pojoCodecProvider = PojoCodecProvider.builder()
                    .register(LaboratoryEntry.class, MedicalCase.class, Patient.class, Procedure.class)
                    .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
                    .build();
            var pojoCodecRegistry = fromProviders(pojoCodecProvider);
            return fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        }

        private static <T> Optional<MongoCollection<T>> getCollection(MongoDatabase db, String collectionName,
                                                                      Class<T> clazz) {
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
}
