package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.Procedure;

import java.util.List;

public interface ProcedureRepo {

    List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId);

    Procedure insertProcedure(Procedure procedure);
}
