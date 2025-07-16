package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.Procedure;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ProcedureRepo implements ProcedureReader, ProcedureWriter {

    private final MongoCollection<Procedure> procedureCollection;

    public ProcedureRepo(MongoCollection<Procedure> procedureCollection) {
        this.procedureCollection = requireNonNull(procedureCollection, "procedure collection must not be null");
    }

    @Override
    public List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId) {
        // TODO: implement
        return List.of();
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");

        return ClientNamespacedWriteModel.insertOne(
                procedureCollection.getNamespace(),
                procedure
        );
    }

    // TODO: rename to createDeletionCommandForProceduresInMedicalCaseOlderThan
    @Override
    public long deleteProceduresInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t) {
        // TODO: implement
        return 0;
    }
}
