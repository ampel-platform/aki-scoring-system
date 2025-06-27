package csv.patient;

import com.opencsv.bean.AbstractBeanField;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@NoArgsConstructor
public final class PatientSexConverter extends AbstractBeanField<PatientSex, String> {
    @Override
    public Object convert(String s) {
        return Arrays.stream(PatientSex.values())
                .filter(patientSex -> patientSex.getRepresentation().equalsIgnoreCase(s))
                .findFirst().orElse(PatientSex.UNKNOWN);
    }
}
