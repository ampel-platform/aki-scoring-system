package csv.patient;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class PatientDeceasedConverter extends AbstractBeanField<Boolean, String> {
    @Override
    protected Object convert(String s) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        return s.equalsIgnoreCase("X");
    }
}
