package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.Patient;

import java.util.Optional;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class PatientRepo implements PatientReader, PatientWriter {

    private final MongoCollection<Patient> patientCollection;

    public PatientRepo(MongoCollection<Patient> patientCollection) {
        this.patientCollection = requireNonNull(patientCollection, "patient collection must not be null");
    }

    @Override
    public Optional<Patient> findLatestPatientById(String patientId) {
        neitherNullNorBlank(patientId, "patient ID must neither be null nor blank");

        return Optional.ofNullable(
                patientCollection.find(Filters.eq("_id", patientId))
                        .sort(Sorts.descending("_ingestionTime"))
                        .first());
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(Patient patient) {
        requireNonNull(patient, "patient must not be null");

        return ClientNamespacedWriteModel.insertOne(
                patientCollection.getNamespace(),
                patient
        );
    }
}
