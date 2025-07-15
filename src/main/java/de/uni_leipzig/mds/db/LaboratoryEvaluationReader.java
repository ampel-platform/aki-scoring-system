package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;

import java.util.List;

public interface LaboratoryEvaluationReader {

    List<LaboratoryEvaluation> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId);

}
