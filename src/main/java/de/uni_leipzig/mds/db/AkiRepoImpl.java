package de.uni_leipzig.mds.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import de.uni_leipzig.mds.db.types.LaboratoryEntry;
import de.uni_leipzig.mds.db.types.MedicalCase;
import de.uni_leipzig.mds.db.types.Patient;
import de.uni_leipzig.mds.db.types.Procedure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static de.uni_leipzig.mds.util.PreconditionChecks.neitherNullNorBlank;
import static java.util.Objects.requireNonNull;

public final class AkiRepoImpl implements AkiRepo {

    private final MongoCollection<LaboratoryEntry> laboratoryCollection;
    private final MongoCollection<MedicalCase> medicalCaseCollection;
    private final MongoCollection<Patient> patientCollection;
    private final MongoCollection<Procedure> procedureCollection;

    AkiRepoImpl(MongoCollection<LaboratoryEntry> laboratoryCollection,
                MongoCollection<MedicalCase> medicalCaseCollection,
                MongoCollection<Patient> patientCollection,
                MongoCollection<Procedure> procedureCollection) {
        this.laboratoryCollection = requireNonNull(laboratoryCollection, "laboratory collection must not be null");
        this.medicalCaseCollection = requireNonNull(medicalCaseCollection, "medical case collection must not be null");
        this.patientCollection = requireNonNull(patientCollection, "patient collection must not be null");
        this.procedureCollection = requireNonNull(procedureCollection, "procedure collection must not be null");
    }

    @Override
    public List<LaboratoryEntry> findAllLaboratoryEntriesInMedicalCase(String medicalCaseId) {
        var labValues = laboratoryCollection.find(Filters.eq("caseId", medicalCaseId));
        return StreamSupport.stream(labValues.spliterator(), false).toList();
    }

    @Override
    public LaboratoryEntry insertLaboratoryEntry(LaboratoryEntry laboratoryEntry) {
        requireNonNull(laboratoryEntry, "laboratoryEntry must not be null");
        var res = laboratoryCollection.insertOne(laboratoryEntry);

        try {
            return StreamSupport.stream(laboratoryCollection
                                    .find(Filters.eq("_id", res.getInsertedId()))
                                    .spliterator(),
                            false)
                    .toList()
                    .getFirst();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("could not find inserted laboratory entry with id '%s'"
                    .formatted(res.getInsertedId()));
        }
    }

    @Override
    public long deleteLaboratoryEntriesInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        var res = laboratoryCollection.deleteMany(
                Filters.and(
                        Filters.eq("caseId", medicalCaseId),
                        Filters.eq("measuringTime", t)
                )
        );

        return res.getDeletedCount();
    }

    @Override
    public Optional<MedicalCase> findMedicalCaseById(String medicalCaseId) {
        neitherNullNorBlank(medicalCaseId, "medical case id must neither be null nor blank");

        return Optional.ofNullable(
                medicalCaseCollection.find(Filters.eq("_id", medicalCaseId))
                        .first());
    }

    @Override
    public MedicalCase upsertMedicalCase(MedicalCase medicalCase) {
        requireNonNull(medicalCase, "medical case must not be null");

        var filter = Filters.eq("_id", medicalCase.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        return medicalCaseCollection.findOneAndReplace(filter, medicalCase, replaceOpts);
    }

    @Override
    public Optional<Patient> findPatientById(String patientId) {
        neitherNullNorBlank(patientId, "patient di must neither be null nor blank");

        return Optional.ofNullable(
                patientCollection.find(Filters.eq("_id", patientId))
                        .first());
    }

    @Override
    public Patient upsertPatient(Patient patient) {
        requireNonNull(patient, "patient must not be null");

        var filter = Filters.eq("_id", patient.id());
        var replaceOpts = new FindOneAndReplaceOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        return patientCollection.findOneAndReplace(filter, patient, replaceOpts);
    }

    @Override
    public List<Procedure> findAllProceduresInMedicalCase(String medicalCaseId) {
        neitherNullNorBlank(medicalCaseId, "medical case id must neither be null nor blank");

        var procedures = procedureCollection.find(Filters.eq("caseId", medicalCaseId));
        return StreamSupport.stream(procedures.spliterator(), false).toList();
    }

    @Override
    public Procedure insertProcedure(Procedure procedure) {
        requireNonNull(procedure, "procedure must not be null");
        var res = procedureCollection.insertOne(procedure);

        try {
            return StreamSupport.stream(procedureCollection
                                    .find(Filters.eq("_id", res.getInsertedId()))
                                    .spliterator(),
                            false)
                    .toList()
                    .getFirst();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("could not find inserted procedure with id '%s'"
                    .formatted(res.getInsertedId()));
        }
    }

    @Override
    public long deleteProceduresInMedicalCaseOlderThan(String medicalCaseId, LocalDateTime t) {
        neitherNullNorBlank(medicalCaseId, "medical case ID must neither be null nor blank");
        requireNonNull(t, "timestamp must not be null");

        var res = procedureCollection.deleteMany(
                Filters.and(
                        Filters.eq("caseId", medicalCaseId),
                        Filters.gt("conductTime", t)
                )
        );

        return res.getDeletedCount();
    }
}
