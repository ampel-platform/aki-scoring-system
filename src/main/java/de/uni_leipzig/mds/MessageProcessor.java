package de.uni_leipzig.mds;

import de.uni_leipzig.mds.data.db.AKIRepo;
import de.uni_leipzig.mds.data.db.LaboratoryValue;
import de.uni_leipzig.mds.data.db.Patient;
import de.uni_leipzig.mds.data.db.Procedure;
import de.uni_leipzig.mds.evaluation.workflow.Parameter;
import de.uni_leipzig.mds.evaluation.workflow.PatientCaseIdentifier;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline.WorkflowPipelineInput;
import de.uni_leipzig.mds.io.connection.KafkaTopic;
import de.uni_leipzig.mds.io.connection.serialization.types.LaboratoryValueMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.PatientMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.ProcedureMessage;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static de.uni_leipzig.mds.WorkflowFactory.PATIENT_FEED_PARAM;
import static de.uni_leipzig.mds.data.db.Codes.*;
import static java.util.Objects.requireNonNull;

/**
 * A processor that is capable of handling messages associated with different {@link KafkaTopic}s that are required to
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

    private final BlockingQueue<Tuple2<KafkaTopic, Object>> incomingMessageChannel;

    private final BlockingQueue<WorkflowPipelineInput> workflowInputChannel;

    private final AKIRepo akiRepo;

    public MessageProcessor(BlockingQueue<Tuple2<KafkaTopic, Object>> incomingMessageChannel,
                            BlockingQueue<WorkflowPipelineInput> workflowInputChannel,
                            AKIRepo akiRepo) {
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
                        message._1().getTopicRepresentation());

                var topic = message._1();
                var payload = message._2();

                switch (topic) {
                    case KafkaTopic.LABORATORY_VALUES -> handleLaboratoryValueMessage((LaboratoryValueMessage) payload);
                    case KafkaTopic.PROCEDURES -> handleProcedureMessage((ProcedureMessage) payload);
                    case KafkaTopic.PATIENTS -> handlePatientMessage((PatientMessage) payload);
                    default -> LOGGER.warn("dropping received message of unknown type");
                }
            } catch (InterruptedException e) {
                // TODO: needs to be handled by a shutdown hook for the thread!
                LOGGER.error("interrupted while waiting for new messages", e);
                break;
            }
        }
    }

    private void handleLaboratoryValueMessage(LaboratoryValueMessage message) {
        if (!getConsiderableLabValueFilter().test(message)) {
            LOGGER.trace("dropping laboratory message since it neither resembles a creatinine nor a vancomycin value");
            return;
        }

        LOGGER.debug("inserting lab value");
        akiRepo.insertLabValue(
                LaboratoryValue.Builder.builder()
                        .code(message.getAnalyteCode())
                        .value(message.getAnalyteValue())
                        .unit("")
                        .time(message.getMeasurementTime())
                        .refs(message.getOrderId(), message.getPatientId(), message.getCaseId())
                        .build());

        if (getCreatinineValueFilter().test(message)) {
            var patientFeed = akiRepo.findPatientFeed(message.getPatientId(), message.getCaseId());

            if (patientFeed.isEmpty()) {
                LOGGER.warn("skipping a workflow trigger because the patient case feed could not be built (maybe due " +
                        "to some missing essential information)");
                return;
            }

            var workflowInputParams = new Parameter()
                    .set(PATIENT_FEED_PARAM, patientFeed.get());

            LOGGER.info("triggering aki workflow due to registered creatinine value");
            workflowInputChannel.add(new WorkflowPipelineInput(
                    new PatientCaseIdentifier(message.getPatientId(), message.getCaseId()),
                    workflowInputParams
            ));
        }
    }

    private Predicate<LaboratoryValueMessage> getConsiderableLabValueFilter() {
        return getCreatinineValueFilter()
                .or(getVancomycinValueFilter());
    }

    private Predicate<LaboratoryValueMessage> getCreatinineValueFilter() {
        return (message) -> LAB_CODES_CREATININE.contains(message.getAnalyteCode().toUpperCase());
    }

    private Predicate<LaboratoryValueMessage> getVancomycinValueFilter() {
        return (message) -> LAB_CODES_VANCOMYCIN.contains(message.getAnalyteCode().toUpperCase());
    }

    private void handleProcedureMessage(ProcedureMessage message) {
        if (!getConsiderableProcedureFilter().test(message)) {
            LOGGER.trace("dropping procedure message since it does not resemble a dialysis procedure");
            return;
        }

        LOGGER.debug("inserting procedure");

        akiRepo.insertProcedure(
                Procedure.Builder.builder()
                        .time(message.getPerformedOn())
                        .code(message.getProcedureCode())
                        .refs(message.getPatientId(), message.getCaseId())
                        .build());
    }

    private Predicate<ProcedureMessage> getConsiderableProcedureFilter() {
        return (message) -> PROCEDURE_CODES_DIALYSIS.contains(message.getProcedureCode().toUpperCase());
    }

    private void handlePatientMessage(PatientMessage message) {
        LOGGER.debug("inserting patient");

        akiRepo.insertPatient(
                Patient.Builder.builder()
                        .patientRef(message.getPatientId())
                        .dateOfBirth(message.getDateOfBirth())
                        .deceased(message.isDeceased())
                        .build());
    }
}
