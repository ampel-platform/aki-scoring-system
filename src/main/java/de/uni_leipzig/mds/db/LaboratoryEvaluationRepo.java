package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.bulk.ClientNamespacedDeleteManyModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class LaboratoryEvaluationRepo implements LaboratoryEvaluationReader, LaboratoryEvaluationWriter {

    private final MongoCollection<LaboratoryEvaluation> laboratoryEvaluationCollection;

    public LaboratoryEvaluationRepo(MongoCollection<LaboratoryEvaluation> laboratoryEvaluationCollection) {
        this.laboratoryEvaluationCollection = requireNonNull(laboratoryEvaluationCollection, "laboratory evaluation collection must not be null");
    }

    @Override
    public List<LaboratoryEvaluation> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId) {
        var labValues = laboratoryEvaluationCollection.find(Filters.eq("medicalCaseId", medicalCaseId));
        return StreamSupport.stream(labValues.spliterator(), false).toList();
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(LaboratoryEvaluation laboratoryEvaluation) {
        requireNonNull(laboratoryEvaluation, "laboratory evaluation must not be null");

        return ClientNamespacedWriteModel.insertOne(
                laboratoryEvaluationCollection.getNamespace(),
                laboratoryEvaluation
        );
    }

    @Override
    public ClientNamespacedDeleteManyModel createDeleteCommandForLaboratoryEntriesInMedicalCaseOlderThan(
            String medicalCaseId, LocalDateTime t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        return ClientNamespacedWriteModel.deleteMany(
                laboratoryEvaluationCollection.getNamespace(),
                Filters.and(
                        Filters.eq("medicalCaseId", medicalCaseId),
                        Filters.eq("measuringTime", t)
                )
        );
    }
}
