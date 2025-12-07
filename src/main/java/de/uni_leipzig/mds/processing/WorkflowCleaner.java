package de.uni_leipzig.mds.processing;

import com.mongodb.ClientBulkWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import com.mongodb.internal.client.model.bulk.ConcreteClientBulkWriteOptions;
import de.uni_leipzig.mds.db.LaboratoryEvaluationWriter;
import de.uni_leipzig.mds.db.ProcedureWriter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

// TODO: add doc
@Slf4j
public final class WorkflowCleaner {

    private final MongoClient mongoClient;
    private final LaboratoryEvaluationWriter laboratoryEvaluationWriter;
    private final ProcedureWriter procedureWriter;

    public WorkflowCleaner(MongoClient mongoClient,
                           LaboratoryEvaluationWriter laboratoryEvaluationWriter,
                           ProcedureWriter procedureWriter) {
        this.mongoClient = requireNonNull(mongoClient, "mongo client must not be null");
        this.laboratoryEvaluationWriter = requireNonNull(laboratoryEvaluationWriter, "lab evaluation writer must not be null");
        this.procedureWriter = requireNonNull(procedureWriter, "procedure writer must not be null");
    }

    public void expireEntriesInMedicalCaseOlderThan(String medicalCaseId, Instant t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        log.info("cleaning up database entries for medical case '{}' that are older than '{}'",
                medicalCaseId, t);

        try {
            List<ClientNamespacedWriteModel> pendingCleanCommands = List.of(
                    procedureWriter.createDeleteCommandForProceduresInMedicalCaseOlderThan(medicalCaseId, t),
                    laboratoryEvaluationWriter.createDeleteCommandForLaboratoryEntriesInMedicalCaseOlderThan(
                            medicalCaseId, t)
            );

            var result = mongoClient.bulkWrite(pendingCleanCommands, new ConcreteClientBulkWriteOptions().ordered(true));
            log.info("cleanup stats: #{} procedures and lab evaluations deleted in medical case '{}' that are older" +
                            " than {}",
                    result.getDeletedCount(), medicalCaseId, t);
        } catch (ClientBulkWriteException e) {
            log.warn("could not clean up database entries for medical case '{}' that are older than '{}'",
                    medicalCaseId, t);
        }
    }
}
