package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.bulk.ClientNamespacedDeleteOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class LaboratoryEvaluationRepo implements LaboratoryEvaluationReader, LaboratoryEvaluationWriter {

    private final MongoCollection<LaboratoryEvaluation> laboratoryEvaluationCollection;

    public LaboratoryEvaluationRepo(MongoCollection<LaboratoryEvaluation> laboratoryEvaluationCollection) {
        this.laboratoryEvaluationCollection = requireNonNull(laboratoryEvaluationCollection, "laboratory evaluation collection must not be null");
    }

    @Override
    public List<LaboratoryEvaluation> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId) {
        // TODO: implement
        return List.of();
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(LaboratoryEvaluation laboratoryEvaluation) {
        requireNonNull(laboratoryEvaluation, "laboratory evaluation must not be null");

        return ClientNamespacedWriteModel.insertOne(
                laboratoryEvaluationCollection.getNamespace(),
                new LaboratoryEvaluation(laboratoryEvaluation, LocalDateTime.now())
        );
    }

    @Override
    public ClientNamespacedDeleteOneModel deleteLaboratoryEntriesInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t) {
        // TODO: implement
        return null;
    }
}
