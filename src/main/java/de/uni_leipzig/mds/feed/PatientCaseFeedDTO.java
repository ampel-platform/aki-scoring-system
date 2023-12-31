package de.uni_leipzig.mds.feed;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@NoArgsConstructor
public class PatientCaseFeedDTO {

    @BsonProperty(value = "_id")
    private ObjectId id;

    private PatientDTO patient;

    @BsonProperty(value = "case_id")
    private String caseId;

    @BsonProperty(value = "creatinine_values")
    private List<LaboratoryValueDTO> creatinineValues;

    @BsonProperty(value = "vancomycin_values")
    private List<LaboratoryValueDTO> vancomycinValues;

    @BsonProperty(value = "dialysis_events")
    private List<ProcedureDTO> dialysisProcedures;

    // intended to be used for optimistic locking (concurrent updates to this object)
    private int version;

    PatientCaseFeedDTO(PatientCaseFeed feed) {
        this.id = (feed.getId());
        this.caseId = feed.getCaseId();
        this.version = feed.getVersion();

        var patient = feed.getPatient();
        var patientDto = new PatientDTO();
        patientDto.setId(patient.getId());
        patientDto.setDateOfBirth(patient.getDateOfBirth());
        patientDto.setDeceased(patient.isDeceased());
        this.patient = patientDto;

        this.creatinineValues = feed.getCreatinineValues().stream()
                .map(v -> {
                    var labValueDto = new LaboratoryValueDTO();
                    labValueDto.setTime(v.getTime());
                    labValueDto.setValue(v.getValue());
                    labValueDto.setUnit(v.getUnit());
                    return labValueDto;
                })
                .toList();

        this.vancomycinValues = feed.getVancomycinValues().stream()
                .map(v -> {
                    var labValueDto = new LaboratoryValueDTO();
                    labValueDto.setTime(v.getTime());
                    labValueDto.setValue(v.getValue());
                    labValueDto.setUnit(v.getUnit());
                    return labValueDto;
                })
                .toList();

        this.dialysisProcedures = feed.getDialysisProcedures().stream()
                .map(p -> {
                    var proDto = new ProcedureDTO();
                    proDto.setTime(p.getTime());
                    proDto.setCode(p.getCode());
                    return proDto;
                })
                .toList();
    }

//    public String toString() {
//        return ("_ID: %s, Patient-ID: %s, Case-ID: %s, deceased: %s, DOB: %s, CreEvents: %s, VancoEvents: %s, " +
//                "DialysisEvents:%s")
//                .formatted(id, patientId, caseId, deceased, dateOfBirth.toString(),
//                        creatinineEvents,
//                        vancomycinEvents,
//                        dialysisEvents);
//    }
}
