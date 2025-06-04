package csv.lab;

import java.util.Comparator;

public class LaboratoryDataMeasuringTimestampComparator implements Comparator<LaboratoryData> {
    @Override
    public int compare(LaboratoryData o1, LaboratoryData o2) {
        return o1.getMeasuredAt().compareTo(o2.getMeasuredAt());
    }
}
