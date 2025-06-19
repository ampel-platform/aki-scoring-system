package csv.medcase;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MedicalCaseStatus {

    PLANNED("P"),
    ACTIVE("I"),
    COMPLETED("E");

    private final String representation;
}
