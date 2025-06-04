package csv.patient;

import java.util.Comparator;

public final class PatientDataCreationComparator implements Comparator<PatientData> {
    @Override
    public int compare(PatientData o1, PatientData o2) {
        return o1.getCreationDate().compareTo(o2.getCreationDate());
    }
}
