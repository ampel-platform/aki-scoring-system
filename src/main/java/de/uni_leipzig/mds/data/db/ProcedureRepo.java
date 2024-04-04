package de.uni_leipzig.mds.data.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * A persistence repository dealing with {@link Procedure}s.
 */
public final class ProcedureRepo {

    public static final String COLLECTION_NAME = "procedures";

    private final MongoCollection<Procedure> collection;

    ProcedureRepo(MongoCollection<Procedure> collection) {
        this.collection = requireNonNull(collection, "collection must not be null");
    }

    /**
     * Finds all {@link Procedure}s for a specific patient and a case within this repository.
     *
     * @param patientId identifies the patient.
     * @param caseId    identifies the case.
     * @return All {@link Procedure}s for this patient within this case.
     */
    public List<Procedure> findAllFor(String patientId, String caseId) {
        requireNonNull(patientId, "patient ID must not be null");
        requireNonNull(caseId, "case ID must not be null");

        var procedures = collection.find(
                Filters.and(
                        Filters.eq("patientIdRef", patientId),
                        Filters.eq("caseIdRef", caseId)
                )
        );

        return StreamSupport.stream(procedures.spliterator(), false).toList();
    }

    /**
     * Inserts a single {@link Procedure} to this repository.
     *
     * @param value procedure to insert.
     */
    public void insert(Procedure value) {
        collection.insertOne(value);
    }
}
