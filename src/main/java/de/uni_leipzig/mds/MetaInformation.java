package de.uni_leipzig.mds;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * The scoring system's meta information. This is just a wrapper around {@link Properties} allowing the callee to only
 * access several pre-defined meta information options.
 */
final class MetaInformation {
    private final Properties properties;

    private MetaInformation(Properties properties) {
        requireNonNull(properties, "properties must not be null");
        this.properties = new Properties(properties);
    }

    /**
     * Reads the scoring system's meta information from a stream containing property definitions as per Java standard
     * for property files.
     *
     * @param propertyStream Stream containing the properties.
     * @return The scoring system's meta information as read from the stream.
     * @throws IOException If the stream cannot be read.
     */
    static MetaInformation readFromPropertyStream(InputStream propertyStream) throws IOException {
        var props = new Properties();
        props.load(propertyStream);

        return new MetaInformation(props);
    }

    /**
     * Gets the value of one of the scoring system's pre-defined properties if it exists.
     *
     * @param property The property.
     * @return Value of the property if it exists.
     */
    Optional<String> getProperty(MetaInformationProperty property) {
        return Optional.ofNullable(properties.getProperty(property.getName()));
    }
}
