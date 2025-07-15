package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.MedicalCase;

import java.time.LocalDateTime;
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
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");

        // TODO: implement
        return Optional.empty();
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(MedicalCase medicalCase) {
        return ClientNamespacedWriteModel.insertOne(
                medicalCaseCollection.getNamespace(),
                new MedicalCase(medicalCase, LocalDateTime.now())
        );
    }
}
