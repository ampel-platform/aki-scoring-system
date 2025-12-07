package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedDeleteManyModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;

import java.time.Instant;

public interface LaboratoryEvaluationWriter {

    ClientNamespacedInsertOneModel createInsertCommand(LaboratoryEvaluation laboratoryEvaluation);

    ClientNamespacedDeleteManyModel createDeleteCommandForLaboratoryEntriesInMedicalCaseOlderThan(
            String medicalCaseId, Instant t);
}
