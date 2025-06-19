package csv.medcase;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

import java.util.Arrays;

public final class MedicalCaseStatusConverter extends AbstractBeanField<MedicalCaseStatus, String> {
    @Override
    protected Object convert(String s) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        return Arrays.stream(MedicalCaseStatus.values())
                .filter(status -> status.getRepresentation().equalsIgnoreCase(s))
                .findFirst().orElseThrow(() -> new CsvConstraintViolationException(("unknown medical status " +
                        "representation '%s'")
                        .formatted(s)));
    }
}
