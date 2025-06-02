package de.uni_leipzig.mds.db;

import java.util.List;

public interface IProcedureRepo {

    void insertProcedure(Procedure procedure);

    List<Procedure> findProceduresInCase(String caseId);
}
