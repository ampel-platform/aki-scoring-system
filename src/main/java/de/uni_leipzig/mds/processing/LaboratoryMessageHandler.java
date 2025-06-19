package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.Context;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.Parameter;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.PatientMedicalCaseIdentifier;
import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.mds.db.AkiRepo;
import de.uni_leipzig.mds.db.types.LaboratoryEntry;
import de.uni_leipzig.mds.processing.context.WorkflowLabValue;
import de.uni_leipzig.mds.processing.context.WorkflowMedicalCase;
import de.uni_leipzig.mds.processing.context.WorkflowPatient;
import de.uni_leipzig.mds.processing.context.WorkflowProcedure;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

import static de.uni_leipzig.mds.processing.WorkflowFactory.MEDICAL_CASE_PARAM;
import static java.util.Objects.requireNonNull;

public final class LaboratoryMessageHandler implements MessageHandler<LaboratoryMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaboratoryMessageHandler.class);

    /**
     * Codes that denote a creatinine value.
     */
    private static final List<String> LAB_CODES_CREATININE = List.of("LCRE_S");

    /**
     * Codes that denote a vancomycin value.
     */
    private static final List<String> LAB_CODES_VANCOMYCIN = List.of("VANC-K_S", "VANC-S_S", "VANC-T_S", "VANC-U_S");

    private final AkiRepo akiRepo;
    private final BlockingQueue<WorkflowTrigger> workflowTriggerQueue;
    private final WorkflowCleaner workflowCleaner;

    public LaboratoryMessageHandler(AkiRepo akiRepo,
                                    BlockingQueue<WorkflowTrigger> workflowTriggerQueue,
                                    WorkflowCleaner workflowCleaner) {
        this.akiRepo = requireNonNull(akiRepo, "aki repository must not be null");
        this.workflowTriggerQueue = requireNonNull(workflowTriggerQueue, "workflow trigger queue must not be null");
        this.workflowCleaner = requireNonNull(workflowCleaner, "workflow cleaner must not be null");
    }

    @Override
    public void handleMessage(Message<String, LaboratoryMessage> message) {
        LOGGER.debug("received laboratory message");
        var labMessagePayload = message.body();

        if (!isConsiderableLaboratoryValue().test(labMessagePayload)) {
            LOGGER.warn("laboratory message `{}` won't get handled since it was filtered out",
                    labMessagePayload.documentId());
            return;
        }

        LOGGER.debug("inserting laboratory value with id `{}`", labMessagePayload.documentId());
        akiRepo.insertLaboratoryEntry(
                new LaboratoryEntry(
                        ObjectId.get(),
                        labMessagePayload.caseId(),
                        labMessagePayload.orderId(),
                        labMessagePayload.analyte(),
                        labMessagePayload.analyteValue(),
                        labMessagePayload.analyteUnit(),
                        labMessagePayload.originatingSystem(),
                        labMessagePayload.analyteMeasuringTimestamp()));

        if (isResemblingCreatinineValue().test(labMessagePayload)) {
            LOGGER.info("triggering workflow execution");
            triggerWorkflowRunWithCleanup(labMessagePayload.caseId(),
                    labMessagePayload.orderId(),
                    labMessagePayload.analyteMeasuringTimestamp().minusDays(7),
                    labMessagePayload.analyteMeasuringTimestamp());
        }
    }

    private Predicate<LaboratoryMessage> isConsiderableLaboratoryValue() {
        var considerableLabValueFilter = isResemblingCreatinineValue()
                .or(isResemblingVancomycinValue());

        return (message) -> {
            var isConsiderableLabValue = considerableLabValueFilter.test(message);
            if (!isConsiderableLabValue) {
                LOGGER.warn("laboratory message `{}` does not resemble a creatinine or vancomycin value",
                        message.documentId());
            }
            return isConsiderableLabValue;
        };
    }

    private Predicate<LaboratoryMessage> isResemblingCreatinineValue() {
        return (message) -> LAB_CODES_CREATININE.contains(message.analyte().toUpperCase());
    }

    private Predicate<LaboratoryMessage> isResemblingVancomycinValue() {
        return (message) -> LAB_CODES_VANCOMYCIN.contains(message.analyte().toUpperCase());
    }

    private Optional<WorkflowMedicalCase> generateMedicalCaseForWorkflowRun(String caseId, String orderId) {
        var medicalCase = akiRepo.findMedicalCaseById(caseId).orElse(null);
        if (medicalCase == null) {
            LOGGER.warn("no associated medical case for laboratory order '{}'", orderId);
            return Optional.empty();
        }
        var patient = akiRepo.findPatientById(medicalCase.patientIdRef()).orElse(null);
        if (patient == null) {
            LOGGER.warn("no associated patient for laboratory order '{}'", orderId);
            return Optional.empty();
        }
        var labValues = akiRepo.findAllLaboratoryEntriesInMedicalCase(caseId)
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

        var procedures = akiRepo.findAllProceduresInMedicalCase(caseId)
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

    private void triggerWorkflowRunWithCleanup(String medicalCaseId, String orderId,
                                    LocalDateTime oldestConsideredDateTime,
                                               LocalDateTime latestConsideredDateTime) {
        generateMedicalCaseForWorkflowRun(medicalCaseId, orderId)
                .map(mc -> mc.filterForValuesBetween(
                        oldestConsideredDateTime,
                        latestConsideredDateTime))
                .ifPresentOrElse(
                        (workflowMedicalCase) -> {
                            if (!workflowMedicalCase.isSufficientForWorkflowRun()) {
                                LOGGER.warn("skipping workflow run for order '{}' because required information is" +
                                        " still missing", orderId);
                            }

                            var workflowInputParams = new Parameter()
                                    .set(MEDICAL_CASE_PARAM, workflowMedicalCase);

                            var workflowContext = new Context();
                            workflowContext.registerScoringSystemResultListener(
                                    msg -> workflowCleaner
                                            .expireEntriesInMedicalCaseOlderThan(medicalCaseId,
                                                    oldestConsideredDateTime));

                            LOGGER.info("triggering aki workflow due to registered creatinine value");
                            workflowTriggerQueue.add(new WorkflowTrigger(
                                    new PatientMedicalCaseIdentifier(workflowMedicalCase.patient().id(),
                                            workflowMedicalCase.caseId())
                                    , workflowContext
                                    , workflowInputParams));
                        },
                        () -> LOGGER.warn("skipping workflow run for order '{}' despite having registered a " +
                                "creatinine value because other information required by the workflow is " +
                                "still missing", orderId)
                );
    }
}
