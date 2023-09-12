package de.uni_leipzig.mds;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.sql.Timestamp;

@Value
@Builder
public class AkiData {

    @NonNull
    String patientNumber;

    @NonNull
    String patientCaseNumber;

    @NonNull
    Timestamp patientBirthdate;

    boolean isDeceased;

    @NonNull
    Timestamp analyteMeasurementTime;

    @NonNull
    Float creatinineValue;

    Float minCreatinineValue48Hours;

    Float minCreatinineValue168Hours;

    boolean hadDialysisInLast168Hours;

    boolean hadVancomycinInLast168Hours;
}
