package mds;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import de.uni_leipzig.mds.evaluation.RuleEvaluationPipeline;
import de.uni_leipzig.mds.io.KafkaResultSerializer;
import de.uni_leipzig.mds.io.ResultWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws PropertyVetoException, SQLException {
        var dbHost = requireNonNull(getenv("DB_HOST"), "database host env var must be specified");
        var dbPort = requireNonNull(getenv("DB_PORT"), "database port env var must be specified");
        var dbName = requireNonNull(getenv("DB_NAME"), "database name env var must be specified");
        var dbUser = requireNonNull(getenv("DB_USER"), "database user env var must be specified");
        var dbPass = requireNonNull(getenv("DB_PASS"), "database password env var must be specified");
        var kafkaNodes = requireNonNull(getenv("KAFKA_NODES"), "kafka broker nodes must be specified");
        var kafkaClientId = requireNonNull(getenv("KAFKA_CLIENT_ID"), "kafka client id must be specified");

        var jdbcUrl = "jdbc:sqlserver://%s:%s;database=%s;encrypt=true;trustServerCertificate=true;"
                .formatted(dbHost, dbPort, dbName);
        LOGGER.info("using the following JDBC connection string: `%s`".formatted(jdbcUrl));
        LOGGER.info("using database user `%s`".formatted(dbUser));

        LOGGER.info("setting up data source for polling");
        var dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPass);
        dataSource.setLoginTimeout(60);
        dataSource.setTestConnectionOnCheckout(true);

        LOGGER.info("setting up rule evaluation pipeline");
        var kafkaResultWriter = ResultWriterFactory.createKafkaResultWriter(parseKafkaNodes(kafkaNodes),
                new KafkaResultSerializer(), kafkaClientId);

        var rulePipeline = RuleEvaluationPipeline.<AkiData>newBuilder()
                .withPoller(new AkiDataPoller(dataSource))
                .withRule(new AkiRule())
                .withResultWriter(kafkaResultWriter)
                .build();

        LOGGER.info("scheduling pipeline invocation every 15s");
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(rulePipeline, 0, 15, SECONDS);
    }

    private static List<InetSocketAddress> parseKafkaNodes(String kafkaNodes) {
        try {
            return Arrays.stream(kafkaNodes.split(","))
                    .map(n -> n.split(":"))
                    .map(nodeAddress -> new InetSocketAddress(nodeAddress[0], Integer.parseInt(nodeAddress[1])))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException("specified Kafka nodes cannot be parsed", e);
        }
    }
}
