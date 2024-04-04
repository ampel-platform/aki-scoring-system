package de.uni_leipzig.mds.data.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * A persistence repository dealing with {@link LaboratoryValue}s.
 */
public final class LaboratoryValueRepo {

    public static final String COLLECTION_NAME = "lab_values";

    private final MongoCollection<LaboratoryValue> collection;

    LaboratoryValueRepo(MongoCollection<LaboratoryValue> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    /**
     * Finds all {@link LaboratoryValue}s for a specific patient and a case within this repository.
     *
     * @param patientId identifies the patient.
     * @param caseId    identifies the case.
     * @return All {@link LaboratoryValue}s for this patient within this case.
     */
    public List<LaboratoryValue> findAllFor(String patientId, String caseId) {
        requireNonNull(patientId, "patient ID must not be null");
        requireNonNull(caseId, "case ID must not be null");

        var labValues = collection.find(
                Filters.and(
                        Filters.eq("patientIdRef", patientId),
                        Filters.eq("caseIdRef", caseId)
                )
        );

        return StreamSupport.stream(labValues.spliterator(), false).toList();
    }

    /**
     * Inserts a single {@link LaboratoryValue} to this repository.
     *
     * @param value lab value to insert.
     */
    public void insert(LaboratoryValue value) {
        collection.insertOne(value);
    }
}
