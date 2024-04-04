package de.uni_leipzig.mds.data.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * A persistence repository dealing with {@link Patient}s.
 */
public final class PatientRepo {

    public static final String COLLECTION_NAME = "patients";

    private final MongoCollection<Patient> collection;

    public PatientRepo(MongoCollection<Patient> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    /**
     * Finds all {@link Patient}s within this repository identified by a patient ID.
     *
     * @param patientId identifies the patient.
     * @return All {@link Patient}s matching the given identifier.
     */
    public List<Patient> find(String patientId) {
        requireNonNull(patientId, "patient ID must not be null");

        var patients = collection.find(
                Filters.eq("patientId", patientId)
        );
        return StreamSupport.stream(patients.spliterator(), false).toList();
    }

    /**
     * Inserts a single {@link Patient} to this repository.
     *
     * @param value patient to insert.
     */
    public void insert(Patient value) {
        collection.insertOne(value);
    }
}
