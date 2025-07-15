package de.uni_leipzig.mds;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.WorkflowScheduler;
import de.uni_leipzig.ampel.sdk.io.kafka.*;
import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.ampel.sdk.io.message.result.ResultMessage;
import de.uni_leipzig.mds.db.DBConnectionSettings;
import de.uni_leipzig.mds.db.MessagePersistenceHandler;
import de.uni_leipzig.mds.db.RepositoryFactory;
import de.uni_leipzig.mds.processing.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var mongoDBHost = requireNonNull(getenv("MONGO_DB_HOST"), "MongoDB host must be specified");
        var mongoDBPort = requireNonNull(getenv("MONGO_DB_PORT"), "MongoDB port must be specified");
        var mongoDBName = requireNonNull(getenv("MONGO_DB_NAME"), "MongoDB name must be specified");
        var kafkaBootstrapServers = requireNonNull(getenv("KAFKA_BOOTSTRAP_SERVERS"), "Kafka bootstrap servers must be specified");
        var kafkaConsumerGroupId = requireNonNull(getenv("KAFKA_CONSUMER_GROUP_ID"), "Kafka consumer group id must be specified");
        var kafkaConsumerCommitTimeoutMs = Long.parseLong(requireNonNull(getenv("KAFKA_CONSUMER_COMMIT_TIMEOUT_MS"), "Kafka consumer commit " +
                "timeout in ms must be specified"));
        var messageBufferSize = Integer.parseInt(requireNonNullElse(getenv("MESSAGE_BUFFER_SIZE"), "100000"));
        var persistenceFlushTimeoutMs = Long.parseLong(requireNonNullElse(getenv("MESSAGE_PERSISTENCE_FLUSH_TIMEOUT_MS"), "200"));

        var workflowTriggerQueue = new LinkedBlockingQueue<WorkflowTrigger>();
        var workflowResultQueue = new LinkedBlockingQueue<Message<String, ResultMessage>>();
        var workflowManager = new Thread(new WorkflowManager(
                workflowTriggerQueue,
                workflowResultQueue,
                WorkflowFactory.createAkiWorkflow(),
                WorkflowScheduler.createVirtualWorkflowScheduler()
        ));


        var sharedTopicReaderConfig = KafkaReaderConfig.builder()
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, kafkaConsumerGroupId)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, kafkaConsumerCommitTimeoutMs)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                .build();


        var dbConnSettings = new DBConnectionSettings(mongoDBHost, mongoDBPort, mongoDBName);
        var mongoClient = RepositoryFactory.getMongoClient(dbConnSettings.getConnectionString());
        var mongoDatabase = RepositoryFactory.getMongoDatabase(dbConnSettings.getConnectionString());

        var patientRepo = RepositoryFactory.getInitializedPatientRepo(mongoDatabase);
        var medicalCaseRepo = RepositoryFactory.getInitializedMedicalCaseRepo(mongoDatabase);
        var labEvaluationRepo = RepositoryFactory.getInitializedLaboratoryEvaluationRepo(mongoDatabase);
        var procedureRepo = RepositoryFactory.getInitializedProcedureRepo(mongoDatabase);
//        var workflowCleaner = new WorkflowCleaner(akiRepo);

        var messagePersistenceHandler = new MessagePersistenceHandler(
                mongoClient,
                medicalCaseRepo,
                patientRepo,
                labEvaluationRepo,
                procedureRepo,
                messageBufferSize,
                persistenceFlushTimeoutMs
        );

        var patientMessageHandler = new PatientMessageHandler(messagePersistenceHandler);
        var patientTopicReader = getTopicReader(KafkaTopics.PATIENTS, kafkaBootstrapServers, sharedTopicReaderConfig,
                patientMessageHandler, PatientMessage.class);

        var medicalCaseMessageHandler = new MedicalCaseMessageHandler(messagePersistenceHandler);
        var medicalCaseTopicReader = getTopicReader(KafkaTopics.MEDICAL_CASES, kafkaBootstrapServers,
                sharedTopicReaderConfig, medicalCaseMessageHandler, MedCaseMessage.class);

        var labMessageHandler = new LaboratoryMessageHandler(messagePersistenceHandler);
        var labTopicReader = getTopicReader(KafkaTopics.LABORATORY, kafkaBootstrapServers, sharedTopicReaderConfig,
                labMessageHandler, LaboratoryMessage.class);

        var procedureMessageHandler = new ProcedureMessageHandler(messagePersistenceHandler);
        var procedureTopicReader = getTopicReader(KafkaTopics.PROCEDURES, kafkaBootstrapServers,
                sharedTopicReaderConfig, procedureMessageHandler, ProcedureMessage.class);

        var resultPublisher = KafkaWriter.<String, ResultMessage>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .useTopic(KafkaTopics.AMPEL_RESULT)
                .withMessagePublishingChannel(workflowResultQueue)
                .withSerializationStrategy(
                        new KafkaSerializationStrategy<>(
                                new StringSerializer(),
                                KafkaSerde.createKafkaSerializer(ResultMessage.class)))
                .withKafkaWriterConfig(
                        KafkaWriterConfig.builder()
                                .setConfigItem(KafkaWriterConfig.KafkaWriterConfigItem.CLIENT_ID, "aki-service")
                                .build())
                .build();

        LOGGER.info("starting workflow manager");
        workflowManager.start();
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
