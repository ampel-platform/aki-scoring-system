package util;

import csv.lab.LaboratoryData;

import java.util.List;

public final class PreProcessingUtil {

    private PreProcessingUtil() {
    }

    public static List<LaboratoryData> filterForNonNaNValues(List<LaboratoryData> labData) {
        return labData.stream()
                .filter(ld -> !ld.getAnalyteValue().isNaN())
                .toList();
    }
}
