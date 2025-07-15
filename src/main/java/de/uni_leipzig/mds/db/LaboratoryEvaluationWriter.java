package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedDeleteOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;

import java.time.LocalDateTime;

public interface LaboratoryEvaluationWriter {

    ClientNamespacedInsertOneModel createInsertCommand(LaboratoryEvaluation laboratoryEvaluation);

    ClientNamespacedDeleteOneModel deleteLaboratoryEntriesInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t);
}
