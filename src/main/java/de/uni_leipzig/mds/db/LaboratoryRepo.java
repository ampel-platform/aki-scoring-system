package de.uni_leipzig.mds.db;

import de.uni_leipzig.mds.db.types.LaboratoryEntry;

import java.util.List;

public interface LaboratoryRepo {

    List<LaboratoryEntry> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId);

    LaboratoryEntry insertLaboratoryEntry(LaboratoryEntry laboratoryEntry);
}
