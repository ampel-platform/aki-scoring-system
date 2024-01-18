package de.uni_leipzig.mds.feed;

import lombok.Getter;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.List;

@Getter
public final class PatientCaseFeedDTO {

    @BsonProperty(value = "_id")
    private final ObjectId id;
    private final PatientDTO patient;
    private final String caseId;
    private final List<LaboratoryValueDTO> creatinineValues;
    private final List<LaboratoryValueDTO> vancomycinValues;
    private final List<ProcedureDTO> dialysisProcedures;
    // intended to be used for optimistic locking (concurrent updates to this object)
    private final int version;

    @BsonCreator
    public PatientCaseFeedDTO(@BsonProperty(value = "_id") ObjectId id,
                              @BsonProperty(value = "patient") PatientDTO patient,
                              @BsonProperty(value = "caseId") String caseId,
                              @BsonProperty(value = "creatinineValues") List<LaboratoryValueDTO> creatinineValues,
                              @BsonProperty(value = "vancomycinValues") List<LaboratoryValueDTO> vancomycinValues,
                              @BsonProperty(value = "dialysisProcedures") List<ProcedureDTO> dialysisProcedures,
                              @BsonProperty(value = "version") int version
    ) {
        this.id = id;
        this.patient = patient;
        this.caseId = caseId;
        this.creatinineValues = List.copyOf(creatinineValues);
        this.vancomycinValues = List.copyOf(vancomycinValues);
        this.dialysisProcedures = List.copyOf(dialysisProcedures);
        this.version = version;
    }

    PatientCaseFeedDTO(PatientCaseFeed feed) {
        this.id = (feed.getId());
        this.caseId = feed.getCaseId();
        this.version = feed.getVersion();
        this.patient = new PatientDTO(feed.getPatient());

        this.creatinineValues = feed.getCreatinineValues().stream()
                .map(LaboratoryValueDTO::new)
                .toList();

        this.vancomycinValues = feed.getVancomycinValues().stream()
                .map(LaboratoryValueDTO::new)
                .toList();

        this.dialysisProcedures = feed.getDialysisProcedures().stream()
                .map(ProcedureDTO::new)
                .toList();
    }
}
