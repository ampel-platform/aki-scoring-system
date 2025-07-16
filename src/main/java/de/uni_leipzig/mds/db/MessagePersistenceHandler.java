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

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

// TODO: this needs to make use of a write-ahead-log approach (WAL) to be fail-safe
@Slf4j
public final class MessagePersistenceHandler {

    private final MongoClient mongoClient;
    private final MedicalCaseWriter medicalCaseWriter;
    private final PatientWriter patientWriter;
    private final LaboratoryEvaluationWriter laboratoryEvaluationWriter;
    private final ProcedureWriter procedureWriter;

    private final BlockingQueue<PendingCommand> pendingCommandsBuffer;
    private final int pendingCommandsBufferCapacity;
    private final Lock pendingCommandsBufferLock;

    private final List<Consumer<List<LaboratoryEvaluation>>> labEvaluationPersistedProspects;

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

        labEvaluationPersistedProspects = new LinkedList<>();

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

    public void registerLabEvaluationPersistedProspect(Consumer<List<LaboratoryEvaluation>> consumer) {
        requireNonNull(consumer, "consumer must not be null");
        labEvaluationPersistedProspects.add(consumer);
    }

    public void writePatient(Patient patient) {
        log.debug("defer persisting information for patient with ID `{}`", patient.patientId());
        var patientWithIngestionTime = new Patient(patient, LocalDateTime.now());
        try {
            pendingCommandsBuffer.put(new PendingCommand(
                    MessageType.PATIENT,
                    patientWriter.createInsertCommand(patientWithIngestionTime),
                    patientWithIngestionTime));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeMedicalCase(MedicalCase medicalCase) {
        log.debug("defer persisting information for medical case with ID `{}`", medicalCase.medicalCaseId());
        var medicalCaseWithIngestionTime = new MedicalCase(medicalCase, LocalDateTime.now());
        try {
            pendingCommandsBuffer.put(new PendingCommand(
                    MessageType.MEDICAL_CASE,
                    medicalCaseWriter.createInsertCommand(medicalCaseWithIngestionTime),
                    medicalCaseWithIngestionTime));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLaboratoryEvaluation(LaboratoryEvaluation laboratoryEvaluation) {
        log.debug("defer persisting information for laboratory evaluation with order ID `{}`",
                laboratoryEvaluation.orderId());
        var laboratoryEvaluationWithIngestionTime = new LaboratoryEvaluation(laboratoryEvaluation,
                LocalDateTime.now());
        try {
            pendingCommandsBuffer.put(new PendingCommand(
                    MessageType.LABORATORY_EVALUATION,
                    laboratoryEvaluationWriter.createInsertCommand(laboratoryEvaluationWithIngestionTime),
                    laboratoryEvaluationWithIngestionTime));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeProcedure(Procedure procedure) {
        log.debug("defer persisting information for procedure with ID `{}`", procedure.procedureId());
        var procedureWithIngestionTime = new Procedure(procedure, LocalDateTime.now());
        try {
            pendingCommandsBuffer.put(new PendingCommand(
                    MessageType.PROCEDURE,
                    procedureWriter.createInsertCommand(procedureWithIngestionTime),
                    procedureWithIngestionTime));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void persistPendingCommands() {
        // needs to be secured since it's possible to be invoked several times if persisting the pending commands
        // takes longer than the timer is configured to wait before triggering this function
        pendingCommandsBufferLock.lock();
        try {
            var pendingCommands = new ArrayList<PendingCommand>(pendingCommandsBufferCapacity);
            pendingCommandsBuffer.drainTo(pendingCommands, pendingCommandsBufferCapacity);

            if (pendingCommands.isEmpty()) {
                log.debug("no commands to persist - skipping persisting step");
                return;
            }

            var commandModels = pendingCommands.stream()
                    .map(pc -> pc.writeModel)
                    .toList();

            log.debug("persisting #{} elements", pendingCommands.size());
            var result = mongoClient.bulkWrite(commandModels, new ConcreteClientBulkWriteOptions().ordered(true));
            log.info("persistence stats: #{} inserted | #{} deleted",
                    result.getInsertedCount(),
                    result.getDeletedCount());

            var persistedLabMessages = pendingCommands.stream()
                    .filter(pc -> pc.messageType.equals(MessageType.LABORATORY_EVALUATION))
                    .map(pc -> (LaboratoryEvaluation) pc.message)
                    .toList();

            // This can be a blocking call since we are holding a lock anyway
            // However, would still be beneficial if it was a rather fast operation.
            notifyProspectsOfPersistedLaboratoryEvaluation(persistedLabMessages);
        } catch (ClientBulkWriteException e) {
            log.error("error while writing patients", e);
            // TODO: put messages back to the buffer if an error occurs - for retry purposes!
            throw new RuntimeException(e);
        } finally {
            pendingCommandsBufferLock.unlock();
        }
    }

    private void notifyProspectsOfPersistedLaboratoryEvaluation(
            List<LaboratoryEvaluation> persistedLabEvaluationMessages) {
        if (labEvaluationPersistedProspects.isEmpty()) {
            log.debug("no registered prospects for persisted laboratory evaluation messages - skipping notifications");
            return;
        }

        log.info("notifying #{} prospects", labEvaluationPersistedProspects.size());
        for (Consumer<List<LaboratoryEvaluation>> prospect : labEvaluationPersistedProspects) {
            prospect.accept(persistedLabEvaluationMessages);
        }
    }

    private enum MessageType {
        MEDICAL_CASE,
        PATIENT,
        LABORATORY_EVALUATION,
        PROCEDURE
    }

    private record PendingCommand(
            MessageType messageType,
            ClientNamespacedWriteModel writeModel,
            Object message
    ) {
    }
}
