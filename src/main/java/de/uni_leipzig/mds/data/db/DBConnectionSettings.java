package de.uni_leipzig.mds.data.db;

import com.mongodb.ConnectionString;
import lombok.Getter;

import static java.util.Objects.requireNonNull;

/**
 * Settings for how to connect to a MongoDB instance.
 */
@Getter
public final class DBConnectionSettings {
    private final String host;
    private final String port;
    private final String dbName;

    public DBConnectionSettings(String host, String port, String dbName) {
        requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        this.host = host;

        requireNonNull(port, "port must not be null");
        if (port.isBlank()) {
            throw new IllegalArgumentException("port must not be blank");
        }
        this.port = port;

        requireNonNull(dbName, "database name must not be null");
        if (dbName.isBlank()) {
            throw new IllegalArgumentException("database name must not be blank");
        }
        this.dbName = dbName;
    }

    /**
     * Gets the connection string as used by MongoDB.
     *
     * @return The connection string.
     */
    public ConnectionString getConnectionString() {
        return new ConnectionString("mongodb://%s:%s".formatted(host, port));
    }
}
