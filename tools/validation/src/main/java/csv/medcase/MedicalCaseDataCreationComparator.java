package csv.medcase;

import java.util.Comparator;

public final class MedicalCaseDataCreationComparator implements Comparator<MedicalCaseData> {
    @Override
    public int compare(MedicalCaseData o1, MedicalCaseData o2) {
        return o1.getCreationDate().compareTo(o2.getCreationDate());
    }
}
