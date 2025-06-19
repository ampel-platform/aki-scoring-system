package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.Procedure;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcedureRepo {

    List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId);

    Procedure insertProcedure(Procedure procedure);

    long deleteProceduresInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t);
}
