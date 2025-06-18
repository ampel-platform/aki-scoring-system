package util;

import csv.lab.LaboratoryData;
import csv.output.OutputData;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PreProcessingUtil {

    private PreProcessingUtil() {
    }

    public static List<LaboratoryData> getLabEntriesWithNaNValues(List<LaboratoryData> labData) {
        return labData.stream()
                .filter(ld -> ld.getAnalyteValue().isNaN())
                .toList();
    }

    public static List<OutputData> filterResultsForOrdersOfExistingLabEntries(List<OutputData> results,
                                                                              List<LaboratoryData> labData) {
        var orderIdsOfExistingLabEntries = labData.stream()
                .map(LaboratoryData::getOrderId)
                .toList();

        var orderIdResultMapping = results.stream()
                .collect(
                        Collectors.toMap(OutputData::getOrderId,
                                r -> r,
                                (prev, next) -> next,
                                HashMap::new));

        return orderIdsOfExistingLabEntries
                .stream()
                .map(orderIdResultMapping::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
