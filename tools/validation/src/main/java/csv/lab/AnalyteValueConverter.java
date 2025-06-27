package csv.lab;

import com.opencsv.bean.AbstractBeanField;

public final class AnalyteValueConverter extends AbstractBeanField<Float, String> {
    @Override
    protected Object convert(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NullPointerException | NumberFormatException e) {
            return Float.NaN;
        }
    }
}
