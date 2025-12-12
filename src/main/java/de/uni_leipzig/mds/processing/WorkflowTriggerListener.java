package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.Context;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.Parameter;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.PatientMedicalCaseIdentifier;
import de.uni_leipzig.mds.db.LaboratoryEvaluationReader;
import de.uni_leipzig.mds.db.MedicalCaseReader;
import de.uni_leipzig.mds.db.PatientReader;
import de.uni_leipzig.mds.db.ProcedureReader;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;
import de.uni_leipzig.mds.processing.context.WorkflowLabValue;
import de.uni_leipzig.mds.processing.context.WorkflowMedicalCase;
import de.uni_leipzig.mds.processing.context.WorkflowPatient;
import de.uni_leipzig.mds.processing.context.WorkflowProcedure;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

import static de.uni_leipzig.mds.processing.LaboratoryMessageHandler.LAB_CODES_CREATININE;
import static de.uni_leipzig.mds.processing.LaboratoryMessageHandler.LAB_CODES_VANCOMYCIN;
import static de.uni_leipzig.mds.processing.WorkflowFactory.MEDICAL_CASE_PARAM;
import static java.util.Objects.requireNonNull;

@Slf4j
public final class WorkflowTriggerListener {

    private final MedicalCaseReader medicalCaseReader;
    private final PatientReader patientReader;
    private final LaboratoryEvaluationReader laboratoryEvaluationReader;
    private final ProcedureReader procedureReader;

    private final WorkflowCleaner workflowCleaner;
    private final BlockingQueue<WorkflowTrigger> workflowTriggerQueue;

    public WorkflowTriggerListener(MedicalCaseReader medicalCaseReader,
                                   PatientReader patientReader,
                                   LaboratoryEvaluationReader laboratoryEvaluationReader,
                                   ProcedureReader procedureReader,
                                   WorkflowCleaner workflowCleaner,
                                   BlockingQueue<WorkflowTrigger> workflowTriggerQueue) {
        this.medicalCaseReader = requireNonNull(medicalCaseReader, "medical case reader must not be null");
        this.patientReader = requireNonNull(patientReader, "patient reader must not be null");
        this.laboratoryEvaluationReader = requireNonNull(laboratoryEvaluationReader, "lab evaluation reader must not be null");
        this.procedureReader = requireNonNull(procedureReader, "procedure reader must not be null");
        this.workflowCleaner = requireNonNull(workflowCleaner, "workflow cleaner must not be null");
        this.workflowTriggerQueue = requireNonNull(workflowTriggerQueue, "workflow trigger queue must not be null");
    }

    public void onLaboratoryEvaluationMessagesPersisted(List<LaboratoryEvaluation> laboratoryEvaluations) {
        requireNonNull(laboratoryEvaluations, "lab evaluations must not be null");

        // TODO: needs to use some sort of WAL as well. In case the system crashes before processing the message
        laboratoryEvaluations
                .forEach(labEvaluation -> {
                    if (isResemblingCreatinineValue().test(labEvaluation)) {
                        triggerWorkflowRunWithCleanup(
                                labEvaluation.medicalCaseId(),
                                labEvaluation.orderId(),
                                labEvaluation.measuringTime()
                                        .minus(7, ChronoUnit.DAYS),
                                labEvaluation.measuringTime());
                    }
                });
    }

    private Predicate<LaboratoryEvaluation> isResemblingCreatinineValue() {
        return (labEvaluation) -> LAB_CODES_CREATININE.contains(labEvaluation.analyte().toUpperCase());
    }

    private Optional<WorkflowMedicalCase> generateMedicalCaseForWorkflowRun(String caseId, String orderId) {
        var startMedical = Instant.now();
        // TODO: this is wrong as the patient information from the time of the laboratory evaluation entry needs to be found!
        var medicalCase = medicalCaseReader.findLatestMedicalCaseById(caseId).orElse(null);
        var endMedical = Instant.now();
        if (medicalCase == null) {
            log.warn("no associated medical case for laboratory order '{}'", orderId);
            return Optional.empty();
        }

        var startPatient = Instant.now();
        // TODO: this is wrong as the patient information from the time of the laboratory evaluation entry needs to be found!
        var patient = patientReader.findLatestPatientById(medicalCase.patientIdRef()).orElse(null);
        var endPatient = Instant.now();
        if (patient == null) {
            log.warn("no associated patient for laboratory order '{}'", orderId);
            return Optional.empty();
        }

        var startLab = Instant.now();
        var labEntries = laboratoryEvaluationReader.findAllLaboratoryEntriesInMedicalCase(caseId);
        var endLab = Instant.now();
        var labValues = labEntries
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

        var startProc = Instant.now();
        var proceduresL = procedureReader.findAllProceduresInMedicalCase(caseId);
        var endProc = Instant.now();
        var procedures = proceduresL
                .stream().map(proc -> new WorkflowProcedure(
                        proc.procedureId(),
                        proc.procedureCode(),
                        proc.conductTime(),
                        proc.cancellationTime()))
                .toList();

        log.debug("Retrieval stats: MedCase - {}ns | Patient - {}ns | Lab - {}ns | Proc - {}ns",
                Duration.between(startMedical, endMedical).toNanos(),
                Duration.between(startPatient, endPatient).toNanos(),
                Duration.between(startLab, endLab).toNanos(),
                Duration.between(startProc, endProc).toNanos());

        return Optional.of(new WorkflowMedicalCase(
                medicalCase.medicalCaseId(),
                medicalCase.creationDate(),
                medicalCase.cancellationDate(),
                new WorkflowPatient(
                        patient.patientId(),
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
                                               Instant oldestConsideredDateTime,
                                               Instant latestConsideredDateTime) {
        generateMedicalCaseForWorkflowRun(medicalCaseId, orderId)
                .map(mc -> mc.filterForValuesBetween(
                        oldestConsideredDateTime,
                        latestConsideredDateTime))
                .ifPresentOrElse(
                        (workflowMedicalCase) -> {
                            if (!workflowMedicalCase.isSufficientForWorkflowRun()) {
                                log.warn("skipping workflow run for order '{}' because required information is" +
                                        " still missing", orderId);
                            }

                            var workflowInputParams = new Parameter()
                                    .set(MEDICAL_CASE_PARAM, workflowMedicalCase);

                            var workflowContext = new Context();
                            workflowContext.registerScoringSystemResultListener(
                                    msg -> workflowCleaner
                                            .expireEntriesInMedicalCaseOlderThan(medicalCaseId,
                                                    oldestConsideredDateTime));

                            log.info("triggering aki workflow due to registered creatinine value");
                            workflowTriggerQueue.add(new WorkflowTrigger(
                                    new PatientMedicalCaseIdentifier(workflowMedicalCase.patient().id(),
                                            workflowMedicalCase.caseId())
                                    , workflowContext
                                    , workflowInputParams));
                        },
                        () -> log.warn("skipping workflow run for order '{}' despite having registered a " +
                                "creatinine value because other information required by the workflow is " +
                                "still missing", orderId)
                );
    }

}
