package de.uni_leipzig.mds;

import de.uni_leipzig.mds.common.types.proto.generated.ampel_event.AmpelEventProtoBuf;
import de.uni_leipzig.mds.db.*;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowScheduler;
import de.uni_leipzig.mds.io.kafka.*;
import de.uni_leipzig.mds.io.message.Message;
import de.uni_leipzig.mds.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.mds.io.message.patient.PatientMessage;
import de.uni_leipzig.mds.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.mds.processing.MessageProcessor;
import de.uni_leipzig.mds.processing.WorkflowFactory;
import io.vavr.Tuple2;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Object>>();

        var laboratoryStreamReader = new Thread(KafkaReader.<String, LaboratoryMessage>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .forTopic(KafkaTopics.LABORATORY)
                .withKafkaReaderConfig(KafkaReaderConfig.builder()
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, kafkaConsumerGroupId)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.ENABLE_AUTO_COMMIT, false)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, Long.parseLong(kafkaConsumerCommitTimeoutMs))
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                        .build())
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(LaboratoryMessage.class)))
                .withMessageConsumer(
                        (Message<String, LaboratoryMessage> m) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopics.LABORATORY, m.body()));
                                LOGGER.trace("enqueued message of topic `{}`", KafkaTopics.LABORATORY.getName());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        })
                .build());

        var patientStreamReader = new Thread(KafkaReader.<String, PatientMessage>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .forTopic(KafkaTopics.PATIENTS)
                .withKafkaReaderConfig(KafkaReaderConfig.builder()
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, kafkaConsumerGroupId)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.ENABLE_AUTO_COMMIT, false)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, Long.parseLong(kafkaConsumerCommitTimeoutMs))
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                        .build())
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(PatientMessage.class)))
                .withMessageConsumer(
                        (Message<String, PatientMessage> m) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopics.PATIENTS, m.body()));
                                LOGGER.trace("enqueued message of topic `{}`", KafkaTopics.PATIENTS.getName());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        })
                .build());

        var procedureStreamReader = new Thread(KafkaReader.<String, ProcedureMessage>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .forTopic(KafkaTopics.PROCEDURES)
                .withKafkaReaderConfig(KafkaReaderConfig.builder()
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, kafkaConsumerGroupId)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.ENABLE_AUTO_COMMIT, false)
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, Long.parseLong(kafkaConsumerCommitTimeoutMs))
                        .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                        .build())
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(ProcedureMessage.class)))
                .withMessageConsumer(
                        (Message<String, ProcedureMessage> m) -> {
                            try {
                                incomingMessages.put(new Tuple2<>(KafkaTopics.PROCEDURES, m.body()));
                                LOGGER.trace("enqueued message of topic `{}`", KafkaTopics.PROCEDURES.getName());
                            } catch (InterruptedException e) {
                                LOGGER.warn("interrupted while enqueueing incoming events", e);
                            }
                        })
                .build());


        LOGGER.info("starting lab event reader");
        laboratoryStreamReader.start();
        LOGGER.info("starting patient event reader");
        patientStreamReader.start();
        LOGGER.info("starting procedure event reader");
        procedureStreamReader.start();

        var dbConnSettings = new DBConnectionSettings(mongoDBHost, mongoDBPort, mongoDBName);
        try (var dbClient = ClientFactory.createDBClient(dbConnSettings.getConnectionString())) {
            var db = dbClient.getDatabase(dbConnSettings.getDbName());
            var akiRepo = new AkiRepo(
                    ProcedureRepo.initialize(db),
                    LabValueRepo.initialize(db),
                    MedicalCaseRepo.initialize(db),
                    PatientRepo.initialize(db)
            );

            var akiWorkflow = WorkflowFactory.createAkiWorkflow();
            var akiWorkflowScheduler = WorkflowScheduler.createVirtualWorkflowScheduler(akiWorkflow);

            var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
            var workflowPipelineOutputQueue = new LinkedBlockingQueue<Message<String, AmpelEventProtoBuf>>();


            var resultStreamPublisher = new Thread(KafkaWriter.<String, AmpelEventProtoBuf>builder()
                    .useBootstrapServers(kafkaBootstrapServers)
                    .useTopic(KafkaTopics.AMPEL_RESULT)
                    .withMessagePublishingChannel(workflowPipelineOutputQueue)
                    .withSerializationStrategy(
                            new KafkaSerializationStrategy<>(
                                    new StringSerializer(),
                                    (topic, data) -> data.toByteArray()))
                    .withKafkaWriterConfig(
                            KafkaWriterConfig.builder()
                                    .setConfigItem(KafkaWriterConfig.KafkaWriterConfigItem.CLIENT_ID, "aki-service")
                                    .build())
                    .build());
            LOGGER.info("starting ampel result publisher");
            resultStreamPublisher.start();

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
