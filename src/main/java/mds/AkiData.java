package mds;

import lombok.Data;
import lombok.NonNull;

import java.sql.Timestamp;

@Data
public class AkiData {

    @NonNull
    private String patientNumber;

    @NonNull
    private String patientCaseNumber;

    @NonNull
    private Timestamp patientBirthdate;

    private boolean isDeceased;

    @NonNull
    private Timestamp analyteMeasurementTime;

    @NonNull
    private Float creatinineValue;

    private Float minCreatinineValue48Hours;

    private Float minCreatinineValue168Hours;

    private boolean hadDialysisInLast168Hours;

    private boolean hadVancomycinInLast168Hours;
}
