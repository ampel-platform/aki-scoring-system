package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.Context;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.Parameter;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.PatientMedicalCaseIdentifier;

// TODO: add doc
public record WorkflowTrigger(
        PatientMedicalCaseIdentifier patientMedicalCaseIdentifier,
        Context workflowContext,
        Parameter workflowInputParameters
) {
}
