package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.Procedure;

import java.time.LocalDateTime;

public interface ProcedureWriter {

    ClientNamespacedInsertOneModel createInsertCommand(Procedure procedure);

    long deleteProceduresInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t);
}
