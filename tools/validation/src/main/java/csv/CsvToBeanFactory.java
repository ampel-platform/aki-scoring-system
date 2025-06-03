package csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import java.io.Reader;

public final class CsvToBeanFactory {

    private CsvToBeanFactory() {
    }

    public static <T> CsvToBean<T> getCsvToBeanTransformer(Reader reader, Class<T> clazz) {
        return new CsvToBeanBuilder<T>(reader)
                .withType(clazz)
                .withSeparator(';')
                .withQuoteChar('"')
                .withStrictQuotes(true)
                .withIgnoreEmptyLine(true)
                .build();
    }
}
