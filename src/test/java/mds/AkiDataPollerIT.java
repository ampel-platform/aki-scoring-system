package mds;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

@Disabled(value = "Can be re-enabled once the DataMart is offered as a Docker image. We don't want to duplicate " +
        "schema creation information as it leads to tight coupling between the projects.")
public class AkiDataPollerIT {

    private static final MSSQLServerContainer mssqlServer = new MSSQLServerContainer(
            "mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    private String readTestResource(String resourceName) throws IOException {
        try (var resourceStream = this.getClass().getResourceAsStream(resourceName)) {
            if (resourceStream == null) {
                throw new RuntimeException("resource could not be read");
            }

            var bufferedReader = new BufferedReader(new InputStreamReader(resourceStream));
            var stringBuilder = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            return stringBuilder.toString();
        }
    }

    private DataSource createSetupDataSource() throws PropertyVetoException, SQLException {
        var dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass(mssqlServer.getDriverClassName());
        dataSource.setJdbcUrl(mssqlServer.getJdbcUrl());
        dataSource.setUser(mssqlServer.getUsername());
        dataSource.setPassword(mssqlServer.getPassword());
        dataSource.setLoginTimeout(60);
        dataSource.setTestConnectionOnCheckout(true);
        return dataSource;
    }

    private void setUpDbSchema(DataSource dataSource) throws SQLException, IOException {
        var schemaSql = readTestResource("database-schema.sql");

        try (var conn = dataSource.getConnection()) {
            conn.createStatement().executeQuery(schemaSql);
        }
    }

    @BeforeEach
    public void setUp() throws PropertyVetoException, SQLException, IOException {
        mssqlServer.start();

        var dataSource = createSetupDataSource();
        setUpDbSchema(dataSource);
    }

    @AfterEach
    public void tearDown() {
        mssqlServer.stop();
    }

    @Test
    public void test() {
    }
}
