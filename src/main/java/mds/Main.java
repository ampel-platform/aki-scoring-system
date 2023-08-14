package mds;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import de.uni_leipzig.mds.evaluation.RuleEvaluationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws PropertyVetoException, SQLException {

        LOGGER.info("setting up data source");
        var dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setJdbcUrl("jdbc:sqlserver://localhost:10433;database=AMPEL;encrypt=true;trustServerCertificate=true;");
        dataSource.setUser("SA");
        // this is fine since its a local setup - going to be replaced by an env var later on.
        dataSource.setPassword("aBc123456!");
        dataSource.setLoginTimeout(60);
        dataSource.setTestConnectionOnCheckout(true);

        LOGGER.info("setting up rule evaluation pipeline");
        var rulePipeline = RuleEvaluationPipeline.<AkiData>newBuilder()
                .withPoller(new AkiDataPoller(dataSource))
                .withRule(new AkiRule())
                .withResultWriter(new AkiConsoleWriter())
                .build();

        LOGGER.info("scheduling pipeline invocation every 15s");
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(rulePipeline, 0, 15, SECONDS);
    }
}
