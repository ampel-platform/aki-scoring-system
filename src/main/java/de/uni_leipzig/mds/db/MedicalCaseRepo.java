package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.MedicalCase;

import java.util.Optional;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class MedicalCaseRepo implements MedicalCaseReader, MedicalCaseWriter {


    private final MongoCollection<MedicalCase> medicalCaseCollection;

    public MedicalCaseRepo(MongoCollection<MedicalCase> medicalCaseCollection) {
        this.medicalCaseCollection = requireNonNull(medicalCaseCollection, "medical case collection must not be null");
    }

    @Override
    public Optional<MedicalCase> findLatestMedicalCaseById(String medicalCaseId) {
        neitherNullNorBlank(medicalCaseId, "medical case id must neither be null nor blank");

        return Optional.ofNullable(
                medicalCaseCollection.find(Filters.eq("medicalCaseId", medicalCaseId))
                        // sort by ingestion time
                        .sort(Sorts.descending("_ingestionTime"))
                        .first());
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(MedicalCase medicalCase) {
        return ClientNamespacedWriteModel.insertOne(
                medicalCaseCollection.getNamespace(),
                medicalCase
        );
    }
}
