package de.uni_leipzig.mds.processing;

import de.uni_leipzig.mds.db.AkiRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

// TODO: add doc
public final class WorkflowCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowCleaner.class);

    private final AkiRepo akiRepo;

    public WorkflowCleaner(AkiRepo akiRepo) {
        this.akiRepo = requireNonNull(akiRepo, "aki repo must not be null");
    }

    public void expireEntriesInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        LOGGER.info("cleaning up database entries for medical case '{}' that are older than '{}'",
                medicalCaseId, t);

        try {
            var numDeletedProcedures = akiRepo.deleteProceduresInMedicalCaseOlderThan(medicalCaseId, t);
            var numDeletedLaboratoryEntries = akiRepo.deleteLaboratoryEntriesInMedicalCaseOlderThan(medicalCaseId, t);

            LOGGER.info("done cleaning up database entries for medical case '{}' that are older than '{}' - '{}'" +
                            "procedures and '{}' laboratory entries were removed",
                    medicalCaseId, t, numDeletedProcedures, numDeletedLaboratoryEntries);
        } catch (Exception e) {
            LOGGER.warn("could not clean up database entries for medical case '{}' that are older than '{}'",
                    medicalCaseId, t);
        }
    }
}
