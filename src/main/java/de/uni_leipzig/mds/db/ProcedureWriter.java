package de.uni_leipzig.mds.db;

import com.mongodb.client.model.bulk.ClientNamespacedDeleteManyModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import de.uni_leipzig.mds.db.types.Procedure;

import java.time.Instant;

public interface ProcedureWriter {

    ClientNamespacedInsertOneModel createInsertCommand(Procedure procedure);

    ClientNamespacedDeleteManyModel createDeleteCommandForProceduresInMedicalCaseOlderThan(
            String medicalCaseId, Instant t);
}
