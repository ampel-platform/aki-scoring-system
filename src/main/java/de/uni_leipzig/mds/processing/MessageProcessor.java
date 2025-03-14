package de.uni_leipzig.mds.processing;

import de.uni_leipzig.mds.db.*;
import de.uni_leipzig.mds.evaluation.workflow.Parameter;
import de.uni_leipzig.mds.evaluation.workflow.PatientCaseIdentifier;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline.WorkflowPipelineInput;
import de.uni_leipzig.mds.io.kafka.KafkaTopics;
import de.uni_leipzig.mds.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.mds.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.mds.io.message.patient.PatientMessage;
import de.uni_leipzig.mds.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.mds.processing.context.WorkflowLabValue;
import de.uni_leipzig.mds.processing.context.WorkflowMedicalCase;
import de.uni_leipzig.mds.processing.context.WorkflowPatient;
import de.uni_leipzig.mds.processing.context.WorkflowProcedure;
import io.vavr.Tuple2;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static de.uni_leipzig.mds.processing.Codes.*;
import static de.uni_leipzig.mds.processing.WorkflowFactory.MEDICAL_CASE_PARAM;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.requireNonNull;

/**
 * A processor that is capable of handling messages associated with different {@link KafkaTopics} that are required to
 * get all mandatory information to run an acute kidney injury (AKI) evaluation.
 * <br><br>
 * It takes care of:
 * <ul>
 *     <li>persisting processed messages</li>
 *     <li>triggering workflow runs by enqueueing workflow pipeline inputs</li>
 * </ul>
 */
public class MessageProcessor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);

    private static final long MAX_LABORATORY_MESSAGE_AGE_IN_DAYS = 7;
    private static final long MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS = 7;

    private final BlockingQueue<Tuple2<KafkaTopics, Object>> incomingMessageChannel;
    private final BlockingQueue<WorkflowPipelineInput> workflowInputChannel;
    private final IAkiRepo akiRepo;

    public MessageProcessor(BlockingQueue<Tuple2<KafkaTopics, Object>> incomingMessageChannel,
                            BlockingQueue<WorkflowPipelineInput> workflowInputChannel,
                            IAkiRepo akiRepo) {
        this.incomingMessageChannel = requireNonNull(incomingMessageChannel, "incoming message channel must not be " +
                "null");
        this.workflowInputChannel = requireNonNull(workflowInputChannel, "workflow message channel must not be null");
        this.akiRepo = requireNonNull(akiRepo, "aki repository must not be null");
    }

    @Override
    public void run() {
        LOGGER.info("starting message processor");

        while (true) {
            try {
                var message = incomingMessageChannel.poll(10, TimeUnit.SECONDS);
                if (message == null) {
                    LOGGER.trace("could not find new messages");
                    continue;
                }

                LOGGER.debug("received message `{}` of topic `{}`", message.hashCode(),
                        message._1().getName());

                var topic = message._1();
                var payload = message._2();

                switch (topic) {
                    case KafkaTopics.LABORATORY -> handleLaboratoryValueMessage(
                            convertMessage(payload, LaboratoryMessage.class));
                    case KafkaTopics.PROCEDURES -> handleProcedureMessage(
                            convertMessage(payload, ProcedureMessage.class));
                    case KafkaTopics.PATIENTS -> handlePatientMessage(
                            convertMessage(payload, PatientMessage.class));
                    case KafkaTopics.MEDICAL_CASES -> handleMedicalCaseMessage(
                            convertMessage(payload, MedCaseMessage.class));
                    default -> LOGGER.warn("dropping received message of unknown type");
                }
            } catch (InterruptedException e) {
                // TODO: needs to be handled by a shutdown hook for the thread!
                LOGGER.error("interrupted while waiting for new messages", e);
                break;
            }
        }
    }

    private <T> T convertMessage(Object message, Class<T> clazz) {
        return clazz.cast(message);
    }

    private void handleLaboratoryValueMessage(LaboratoryMessage message) {
        if (!getConsiderableLabValueFilter().test(message)) {
            LOGGER.trace("dropping laboratory message since it neither resembles a creatinine nor a vancomycin value");
            return;
        }

        if (isLabValueOlderThanMaxAllowedAge().test(message)) {
            LOGGER.warn("dropping laboratory message since it's older than {} days and thus is not relevant anymore",
                    MAX_LABORATORY_MESSAGE_AGE_IN_DAYS);
            return;
        }

        LOGGER.debug("inserting lab value");
        akiRepo.insertLabValue(
                new LabValue(
                        ObjectId.get(),
                        message.caseId(),
                        message.orderId(),
                        message.analyte(),
                        message.analyteValue(),
                        message.analyteUnit(),
                        message.originatingSystem(),
                        message.analyteMeasuringTimestamp()));

        if (getCreatinineValueFilter().test(message)) {
            generateMedicalCaseForWorkflowRun(message.caseId())
                    .map(mc -> mc.filterForValuesBetween(
                            message.analyteMeasuringTimestamp().minusDays(7),
                            message.analyteMeasuringTimestamp()))
                    .ifPresentOrElse(this::triggerWorkflowRun,
                            () -> LOGGER.warn("skipping workflow run despite having registered a creatinine value" +
                                    " because other information required by the workflow is still missing"));
        }
    }

    private void triggerWorkflowRun(WorkflowMedicalCase workflowMedicalCase) {
        if (!workflowMedicalCase.isSufficientForWorkflowRun()) {
            LOGGER.warn("skipping workflow run because required information is still missing");
        }

        var workflowInputParams = new Parameter()
                .set(MEDICAL_CASE_PARAM, workflowMedicalCase);

        LOGGER.info("triggering aki workflow due to registered creatinine value");
        workflowInputChannel.add(new WorkflowPipelineInput(
                new PatientCaseIdentifier(workflowMedicalCase.patient().id(), workflowMedicalCase.caseId()),
                workflowInputParams
        ));
    }

    private Predicate<LaboratoryMessage> getConsiderableLabValueFilter() {
        return getCreatinineValueFilter()
                .or(getVancomycinValueFilter());
    }

    private Predicate<LaboratoryMessage> isLabValueOlderThanMaxAllowedAge() {
        return (message) -> {
            var ageInDays = Math.abs(message.analyteMeasuringTimestamp().until(LocalDateTime.now(), DAYS));
            return ageInDays > MAX_LABORATORY_MESSAGE_AGE_IN_DAYS;
        };
    }

    private Predicate<LaboratoryMessage> getCreatinineValueFilter() {
        return (message) -> LAB_CODES_CREATININE.contains(message.analyte().toUpperCase());
    }

    private Predicate<LaboratoryMessage> getVancomycinValueFilter() {
        return (message) -> LAB_CODES_VANCOMYCIN.contains(message.analyte().toUpperCase());
    }

    private void handleProcedureMessage(ProcedureMessage message) {
        if (!getConsiderableProcedureFilter().test(message)) {
            LOGGER.trace("dropping procedure message since it does not resemble a dialysis procedure");
            return;
        }

        if (isProcedureOlderThanMaxAllowedAgeInDays().test(message)) {
            LOGGER.warn("dropping procedure message since it's older than {} days and thus not relevant anymore",
                    MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS);
            return;
        }

        LOGGER.debug("inserting procedure with id `{}`", message.procedureId());

        akiRepo.insertProcedure(
                new Procedure(
                        ObjectId.get(),
                        message.caseId(),
                        message.procedureId(),
                        message.procedureCode(),
                        message.performedOn(),
                        message.cancellationDate()));
    }

    private Predicate<ProcedureMessage> getConsiderableProcedureFilter() {
        return (message) -> PROCEDURE_CODES_DIALYSIS.contains(message.procedureCode().toUpperCase());
    }

    private Predicate<ProcedureMessage> isProcedureOlderThanMaxAllowedAgeInDays() {
        return (message) -> {
            var ageInDays = Math.abs(message.performedOn().until(LocalDateTime.now(), DAYS));
            return ageInDays > MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS;
        };
    }

    private void handlePatientMessage(PatientMessage message) {
        LOGGER.debug("inserting patient with id `{}`", message.patientId());

        akiRepo.upsertPatient(
                new Patient(
                        message.patientId(),
                        message.dateOfBirth(),
                        message.deceased(),
                        message.dischargeTimestamp()));
    }

    private void handleMedicalCaseMessage(MedCaseMessage medicalCase) {
        LOGGER.debug("inserting medical case with id `{}`", medicalCase.caseId());

        akiRepo.upsertMedicalCase(
                new MedicalCase(
                        medicalCase.caseId(),
                        medicalCase.patientId(),
                        medicalCase.creationDate(),
                        medicalCase.cancellationDate()));
    }

    private Optional<WorkflowMedicalCase> generateMedicalCaseForWorkflowRun(String caseId) {
        var medicalCase = akiRepo.findMedicalCase(caseId).orElse(null);
        if (medicalCase == null) {
            return Optional.empty();
        }
        var patient = akiRepo.findPatientInCase(caseId).orElse(null);
        if (patient == null) {
            return Optional.empty();
        }
        var labValues = akiRepo.findLabValuesInCase(caseId)
                .stream().map(lv -> new WorkflowLabValue(
                        lv.orderId(),
                        lv.analyte(),
                        lv.analyteValue(),
                        lv.analyteUnit(),
                        lv.originatingSystem(),
                        lv.measuringTime()))
                .toList();

        var creatinineValues = labValues.stream().filter(
                        lv -> LAB_CODES_CREATININE.contains(lv.analyte().toUpperCase()))
                .toList();

        var vancomycinValues = labValues.stream().filter(
                        lv -> LAB_CODES_VANCOMYCIN.contains(lv.analyte().toUpperCase()))
                .toList();

        var procedures = akiRepo.findProceduresInCase(caseId)
                .stream().map(proc -> new WorkflowProcedure(
                        proc.procedureId(),
                        proc.procedureCode(),
                        proc.conductTime(),
                        proc.cancellationTime()))
                .toList();

        return Optional.of(new WorkflowMedicalCase(
                medicalCase.id(),
                medicalCase.creationDate(),
                medicalCase.cancellationDate(),
                new WorkflowPatient(
                        patient.id(),
                        patient.dateOfBirth(),
                        patient.deceased(),
                        patient.dischargeTime()
                ),
                creatinineValues,
                vancomycinValues,
                procedures
        ));
    }
}
