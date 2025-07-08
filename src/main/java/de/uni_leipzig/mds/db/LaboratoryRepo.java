package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.LaboratoryEntry;

import java.time.LocalDateTime;
import java.util.List;

public interface LaboratoryRepo {

    List<LaboratoryEntry> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId);

    void insertLaboratoryEntry(LaboratoryEntry laboratoryEntry);

    long deleteLaboratoryEntriesInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t);
}
