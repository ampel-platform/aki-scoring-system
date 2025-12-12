package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.bulk.ClientNamespacedDeleteManyModel;
import com.mongodb.client.model.bulk.ClientNamespacedInsertOneModel;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import de.uni_leipzig.mds.db.types.Procedure;

import java.time.Instant;
import java.util.List;
import java.util.stream.StreamSupport;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class ProcedureRepo implements ProcedureReader, ProcedureWriter {

    private final MongoCollection<Procedure> procedureCollection;

    public ProcedureRepo(MongoCollection<Procedure> procedureCollection) {
        this.procedureCollection = requireNonNull(procedureCollection, "procedure collection must not be null");
    }

    @Override
    public List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId) {
        neitherNullNorBlank(medicalCaseId, "medical case id must neither be null nor blank");

        var procedures = procedureCollection.find(Filters.eq("medicalCaseId", medicalCaseId));
        return StreamSupport.stream(procedures.spliterator(), false).toList();
    }

    @Override
    public ClientNamespacedInsertOneModel createInsertCommand(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");

        return ClientNamespacedWriteModel.insertOne(
                procedureCollection.getNamespace(),
                procedure
        );
    }

    @Override
    public ClientNamespacedDeleteManyModel createDeleteCommandForProceduresInMedicalCaseOlderThan(
            String medicalCaseId, Instant t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        return ClientNamespacedWriteModel.deleteMany(
                procedureCollection.getNamespace(),
                Filters.and(
                        Filters.eq("medicalCaseId", medicalCaseId),
                        Filters.gt("conductTime", t)
                )
        );
    }
}
