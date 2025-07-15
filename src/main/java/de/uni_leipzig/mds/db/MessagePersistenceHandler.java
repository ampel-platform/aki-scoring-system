package de.uni_leipzig.mds.db;

import com.mongodb.ClientBulkWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.bulk.ClientNamespacedWriteModel;
import com.mongodb.internal.client.model.bulk.ConcreteClientBulkWriteOptions;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;
import de.uni_leipzig.mds.db.types.MedicalCase;
import de.uni_leipzig.mds.db.types.Patient;
import de.uni_leipzig.mds.db.types.Procedure;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Objects.requireNonNull;

// TODO: this needs to make use of a write-ahead-log approach (WAL) to be fail-safe
@Slf4j
public final class MessagePersistenceHandler {

    private final MongoClient mongoClient;
    private final MedicalCaseWriter medicalCaseWriter;
    private final PatientWriter patientWriter;
    private final LaboratoryEvaluationWriter laboratoryEvaluationWriter;
    private final ProcedureWriter procedureWriter;

    private final BlockingQueue<Map.Entry<MessageType, ClientNamespacedWriteModel>> pendingCommandsBuffer;
    private final int pendingCommandsBufferCapacity;
    private final Lock pendingCommandsBufferLock;

    public MessagePersistenceHandler(MongoClient mongoClient,
                                     MedicalCaseWriter medicalCaseWriter,
                                     PatientWriter patientWriter,
                                     LaboratoryEvaluationWriter laboratoryEvaluationWriter,
                                     ProcedureWriter procedureWriter,
                                     int messageCapacity,
                                     long persistenceTimeoutMs
    ) {
        this.mongoClient = requireNonNull(mongoClient, "mongo client must not be null");
        this.medicalCaseWriter = requireNonNull(medicalCaseWriter, "medical case writer must not be null");
        this.patientWriter = requireNonNull(patientWriter, "patient writer must not be null");
        this.laboratoryEvaluationWriter = requireNonNull(laboratoryEvaluationWriter, "laboratory writer must not be null");
        this.procedureWriter = requireNonNull(procedureWriter, "procedure writer must not be null");

        if (messageCapacity <= 0) {
            throw new IllegalArgumentException("message capacity must be greater than zero");
        }
        pendingCommandsBuffer = new ArrayBlockingQueue<>(messageCapacity);
        pendingCommandsBufferCapacity = messageCapacity;
        pendingCommandsBufferLock = new ReentrantLock();

        if (persistenceTimeoutMs <= 0) {
            throw new IllegalArgumentException("persistence timeout (ms) must be greater than zero");
        }
        var consumePendingCommandsBufferTimer = new Timer();
        consumePendingCommandsBufferTimer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        persistPendingCommands();
                    }
                },
                0,
                persistenceTimeoutMs);
    }

    public void writePatient(Patient patient) {
        log.debug("defer persisting information for patient with ID `{}`", patient.patientId());
        try {
            pendingCommandsBuffer.put(Map.entry(
                    MessageType.PATIENT,
                    patientWriter.createInsertCommand(patient)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeMedicalCase(MedicalCase medicalCase) {
        log.debug("defer persisting information for medical case with ID `{}`", medicalCase.medicalCaseId());
        try {
            pendingCommandsBuffer.put(Map.entry(
                    MessageType.MEDICAL_CASE,
                    medicalCaseWriter.createInsertCommand(medicalCase)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLaboratoryEvaluation(LaboratoryEvaluation laboratoryEvaluation) {
        log.debug("defer persisting information for laboratory evaluation with order ID `{}`",
                laboratoryEvaluation.orderId());
        try {
            pendingCommandsBuffer.put(Map.entry(
                    MessageType.LABORATORY_EVALUATION,
                    laboratoryEvaluationWriter.createInsertCommand(laboratoryEvaluation)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeProcedure(Procedure procedure) {
        log.debug("defer persisting information for procedure with ID `{}`", procedure.procedureId());
        try {
            pendingCommandsBuffer.put(Map.entry(
                    MessageType.PROCEDURE,
                    procedureWriter.createInsertCommand(procedure)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void persistPendingCommands() {
        // needs to be secured since it's possible to be invoked several times if persisting the pending commands
        // takes longer than the timer is configured to wait before triggering this function
        pendingCommandsBufferLock.lock();
        try {
            var pendingCommandsByMessageType = new ArrayList<Map.Entry<MessageType, ClientNamespacedWriteModel>>
                    (pendingCommandsBufferCapacity);
            pendingCommandsBuffer.drainTo(pendingCommandsByMessageType, pendingCommandsBufferCapacity);

            if (pendingCommandsByMessageType.isEmpty()) {
                log.debug("no commands to persist - skipping persisting step");
                return;
            }

            var pendingCommands = pendingCommandsByMessageType.stream()
                    .map(Map.Entry::getValue)
                    .toList();

            log.debug("persisting #{} elements", pendingCommandsByMessageType.size());
            var result = mongoClient.bulkWrite(pendingCommands, new ConcreteClientBulkWriteOptions().ordered(true));
            log.info("persistence stats: #{} inserted | #{} deleted",
                    result.getInsertedCount(),
                    result.getDeletedCount());

//            notifyProspectsOfPersistedLaboratoryEvaluation(persistedLaboratoryEvaluationMessages);
        } catch (ClientBulkWriteException e) {
            log.error("error while writing patients", e);
            throw new RuntimeException(e);
        } finally {
            pendingCommandsBufferLock.unlock();
        }
    }

    private void notifyProspectsOfPersistedLaboratoryEvaluation() {
        log.info("notifying prospects");
    }

    private enum MessageType {
        MEDICAL_CASE,
        PATIENT,
        LABORATORY_EVALUATION,
        PROCEDURE
    }

    public record LaboratoryEvaluationPersistedNotification(

    ) {
    }
}
