package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public class PatientRepo {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcedureRepo.class);

    private static final String PATIENT_COLLECTION_NAME = "patient";

    private final MongoCollection<Patient> collection;

    private PatientRepo(MongoCollection<Patient> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    public static PatientRepo initialize(MongoDatabase db) {
        requireNonNull(db, "database must not be null");

        var patientCollection = RepoUtil.getCollection(db, PATIENT_COLLECTION_NAME, Patient.class)
                .or(() -> createPatientCollection(db))
                .orElseThrow(() -> new RuntimeException("cannot create patient collection"));

        LOGGER.info("initialized patient repo");
        return new PatientRepo(patientCollection);
    }

    private static Optional<MongoCollection<Patient>> createPatientCollection(MongoDatabase db) {
        db.createCollection(PATIENT_COLLECTION_NAME);

        return Optional.of(
                db.getCollection(PATIENT_COLLECTION_NAME)
                        .withDocumentClass(Patient.class)
                        .withWriteConcern(ACKNOWLEDGED));
    }

    Optional<Patient> findById(String patientId) {
        neitherNullNorBlank(patientId, "patient id must neither be null nor blank");

        return Optional.ofNullable(
                collection.find(Filters.eq("_id", patientId))
                        .first());
    }

    void upsertPatient(Patient patient) {
        requireNonNull(patient, "patient must not be null");

        var filter = Filters.eq("_id", patient.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true);

        collection.findOneAndReplace(filter, patient, replaceOpts);
    }
}
