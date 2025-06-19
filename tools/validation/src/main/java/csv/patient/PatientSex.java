package csv.patient;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PatientSex {

    MALE("1"),
    FEMALE("2"),
    UNKNOWN("3");

    private final String representation;
}
