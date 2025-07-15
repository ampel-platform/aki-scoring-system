package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.Procedure;

import java.util.List;

public interface ProcedureReader {
    List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId);
}
