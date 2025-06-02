package de.uni_leipzig.mds;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.WorkflowPipeline;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.WorkflowScheduler;
import de.uni_leipzig.ampel.sdk.io.kafka.*;
import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.ampel.sdk.io.message.result.ResultMessage;
import de.uni_leipzig.mds.db.*;
import de.uni_leipzig.mds.processing.*;
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


        //var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Message<String, ?>>>();
        var sharedTopicReaderConfig = KafkaReaderConfig.builder()
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, kafkaConsumerGroupId)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.ENABLE_AUTO_COMMIT, false)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, Long.parseLong(kafkaConsumerCommitTimeoutMs))
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                .build();

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
            var workflowPipelineOutputQueue = new LinkedBlockingQueue<Message<String, ResultMessage>>();

            var workflowScheduler = new Thread(WorkflowPipeline.builder()
                    .withIOQueues(workflowPipelineInputQueue, workflowPipelineOutputQueue)
                    .withScheduler(akiWorkflowScheduler)
                    .build());

            var labMessageHandler = new LaboratoryMessageHandler(akiRepo, workflowPipelineInputQueue);
            var labTopicReader = getTopicReader(KafkaTopics.LABORATORY, kafkaBootstrapServers, sharedTopicReaderConfig,
                    labMessageHandler, LaboratoryMessage.class);

            var medicalCaseMessageHandler = new MedicalCaseMessageHandler(akiRepo);
            var medicalCaseTopicReader = getTopicReader(KafkaTopics.MEDICAL_CASES, kafkaBootstrapServers,
                    sharedTopicReaderConfig, medicalCaseMessageHandler, MedCaseMessage.class);

            var patientMessageHandler = new PatientMessageHandler(akiRepo);
            var patientTopicReader = getTopicReader(KafkaTopics.PATIENTS, kafkaBootstrapServers, sharedTopicReaderConfig,
                    patientMessageHandler, PatientMessage.class);

            var procedureMessageHandler = new ProcedureMessageHandler(akiRepo);
            var procedureTopicReader = getTopicReader(KafkaTopics.PROCEDURES, kafkaBootstrapServers,
                    sharedTopicReaderConfig, procedureMessageHandler, ProcedureMessage.class);

            var resultPublisher = KafkaWriter.<String, ResultMessage>builder()
                    .useBootstrapServers(kafkaBootstrapServers)
                    .useTopic(KafkaTopics.AMPEL_RESULT)
                    .withMessagePublishingChannel(workflowPipelineOutputQueue)
                    .withSerializationStrategy(
                            new KafkaSerializationStrategy<>(
                                    new StringSerializer(),
                                    KafkaSerde.createKafkaSerializer(ResultMessage.class)))
                    .withKafkaWriterConfig(
                            KafkaWriterConfig.builder()
                                    .setConfigItem(KafkaWriterConfig.KafkaWriterConfigItem.CLIENT_ID, "aki-service")
                                    .build())
                    .build();

            LOGGER.info("starting workflow scheduler");
            workflowScheduler.start();
            LOGGER.info("starting lab event reader");
            labTopicReader.start();
            LOGGER.info("starting patient event reader");
            patientTopicReader.start();
            LOGGER.info("starting medical case event reader");
            medicalCaseTopicReader.start();
            LOGGER.info("starting procedure event reader");
            procedureTopicReader.start();
            LOGGER.info("starting ampel result publisher");
            resultPublisher.run();
        }
    }

    private static <T> Thread getTopicReader(KafkaTopics topic, String bootstrapServers, KafkaReaderConfig readerConfig,
                                             MessageHandler<T> messageHandler, Class<T> messageType) {
        return new Thread(KafkaReader.<String, T>builder()
                .useBootstrapServers(bootstrapServers)
                .forTopic(topic)
                .withKafkaReaderConfig(readerConfig)
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(messageType)))
                .withMessageConsumer(
                        messageHandler::handleMessage)
                .build());
    }
}
