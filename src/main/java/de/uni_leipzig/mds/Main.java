package de.uni_leipzig.mds;

import de.uni_leipzig.mds.common.types.proto.generated.ampel_event.AmpelEventProtoBuf;
import de.uni_leipzig.mds.data.db.ClientFactory;
import de.uni_leipzig.mds.data.db.DBConnectionSettings;
import de.uni_leipzig.mds.data.db.RepoFactory;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowScheduler;
import de.uni_leipzig.mds.io.connection.*;
import de.uni_leipzig.mds.io.connection.serialization.PatientCaseKeyDeserializer;
import de.uni_leipzig.mds.io.connection.serialization.PatientCaseKeySerializer;
import de.uni_leipzig.mds.io.connection.serialization.types.LaboratoryValueMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.PatientMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.ProcedureMessage;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var mongoDBHost = requireNonNull(getenv("MONGO_DB_HOST"), "MongoDB host must be specified");
        var mongoDBPort = requireNonNull(getenv("MONGO_DB_PORT"), "MongoDB port must be specified");
        var mongoDBName = requireNonNull(getenv("MONGO_DB_NAME"), "MongoDB name must be specified");
        var kafkaBootstrapServers = requireNonNull(getenv("KAFKA_BOOTSTRAP_SERVERS"), "Kafka bootstrap servers must be specified");
        var kafkaConsumerGroupId = requireNonNull(getenv("KAFKA_CONSUMER_GROUP_ID"), "Kafka consumer group id must be specified");
        var kafkaConsumerCommitTimeoutMs = requireNonNull(getenv("KAFKA_CONSUMER_COMMIT_TIMEOUT_MS"), "Kafka consumer commit " +
                "timeout in ms must be specified");


        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopic, Object>>();
        var patientCaseKeyDeserializer = new PatientCaseKeyDeserializer();
        var patientCaseKeySerializer = new PatientCaseKeySerializer();

        var labEventReader = new Thread(KafkaReadConnector.create(
                new KafkaReadConnectorSettings<LaboratoryValueMessage>(
                        List.of(kafkaBootstrapServers),
                        kafkaConsumerGroupId + "_L",
                        KafkaTopic.LABORATORY_VALUES,
                        Duration.ofMillis(Long.parseLong(kafkaConsumerCommitTimeoutMs)),
                        patientCaseKeyDeserializer,
                        (message) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopic.LABORATORY_VALUES, message.payload()));
                                LOGGER.trace("enqueued message of topic `{}`",
                                        KafkaTopic.LABORATORY_VALUES.getTopicRepresentation());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        }
                )
        ));

        var patientEventReader = new Thread(KafkaReadConnector.create(
                new KafkaReadConnectorSettings<PatientMessage>(
                        List.of(kafkaBootstrapServers),
                        kafkaConsumerGroupId + "_PAT",
                        KafkaTopic.PATIENTS,
                        Duration.ofMillis(Long.parseLong(kafkaConsumerCommitTimeoutMs)),
                        patientCaseKeyDeserializer,
                        (message) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopic.PATIENTS, message.payload()));
                                LOGGER.trace("enqueued message of topic `{}`",
                                        KafkaTopic.PATIENTS.getTopicRepresentation());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        }
                )
        ));

        var procedureEventReader = new Thread(KafkaReadConnector.create(
                new KafkaReadConnectorSettings<ProcedureMessage>(
                        List.of(kafkaBootstrapServers),
                        kafkaConsumerGroupId + "_PROC",
                        KafkaTopic.PROCEDURES,
                        Duration.ofMillis(Long.parseLong(kafkaConsumerCommitTimeoutMs)),
                        patientCaseKeyDeserializer,
                        (message) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopic.PROCEDURES, message.payload()));
                                LOGGER.trace("enqueued message of topic `{}`",
                                        KafkaTopic.PROCEDURES.getTopicRepresentation());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        }
                )
        ));

        LOGGER.info("starting lab event reader");
        labEventReader.start();
        LOGGER.info("starting patient event reader");
        patientEventReader.start();
        LOGGER.info("starting procedure event reader");
        procedureEventReader.start();

        var dbConnSettings = new DBConnectionSettings(mongoDBHost, mongoDBPort, mongoDBName);
        try (var dbClient = ClientFactory.createDBClient(dbConnSettings.getConnectionString())) {
            var repoFactory = new RepoFactory(dbClient.getDatabase(dbConnSettings.getDbName()));
            var akiRepo = repoFactory.createAkiRepo();

            var akiWorkflow = WorkflowFactory.createAkiWorkflow();
            var akiWorkflowScheduler = WorkflowScheduler.createVirtualWorkflowScheduler(akiWorkflow);

            var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
            var workflowPipelineOutputQueue = new LinkedBlockingQueue<AmpelMessage<AmpelEventProtoBuf>>();

            var resultPublisher = new Thread(KafkaWriteConnector.create(
                    new KafkaWriteConnectorSettings(
                            List.of(kafkaBootstrapServers),
                            "aki-ruleset",
                            KafkaTopic.AMPEL_RESULT,
                            patientCaseKeySerializer
                    ),
                    workflowPipelineOutputQueue
            ));

            LOGGER.info("starting ampel result publisher");
            resultPublisher.start();

            var pipeline = WorkflowPipeline.builder()
                    .withIOQueues(workflowPipelineInputQueue, workflowPipelineOutputQueue)
                    .withScheduler(akiWorkflowScheduler)
                    .build();

            var workflowScheduler = new Thread(pipeline);
            LOGGER.info("starting worklow scheduler");
            workflowScheduler.start();

            var messageProcessor = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, akiRepo);
            messageProcessor.run();
        }
    }
}
