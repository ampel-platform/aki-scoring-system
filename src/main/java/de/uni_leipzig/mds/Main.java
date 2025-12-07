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

import java.io.IOException;
import java.time.ZoneId;
import java.util.concurrent.LinkedBlockingQueue;

import static de.uni_leipzig.ampel.sdk.io.kafka.KafkaConsumerConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET;
import static de.uni_leipzig.ampel.sdk.io.kafka.KafkaConsumerConfig.KafkaReaderConfigItem.GROUP_ID;
import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        var mongoDBHost = requireNonNull(getenv("MONGO_DB_HOST"), "MongoDB host must be specified");
        var mongoDBPort = requireNonNull(getenv("MONGO_DB_PORT"), "MongoDB port must be specified");
        var mongoDBName = requireNonNull(getenv("MONGO_DB_NAME"), "MongoDB name must be specified");
        var kafkaBootstrapServers = requireNonNull(getenv("KAFKA_BOOTSTRAP_SERVERS"), "Kafka bootstrap servers must be specified");
        var kafkaConsumerGroupId = requireNonNull(getenv("KAFKA_CONSUMER_GROUP_ID"), "Kafka consumer group id must be specified");
        var kafkaConsumerCommitTimeoutMs = Long.parseLong(requireNonNull(getenv("KAFKA_CONSUMER_COMMIT_TIMEOUT_MS"), "Kafka consumer commit " +
                "timeout in ms must be specified"));
        var messageBufferSize = Integer.parseInt(requireNonNullElse(getenv("MESSAGE_BUFFER_SIZE"), "100000"));
        var persistenceFlushTimeoutMs = Long.parseLong(requireNonNullElse(getenv("MESSAGE_PERSISTENCE_FLUSH_TIMEOUT_MS"), "200"));
        var timezone = ZoneId.of(requireNonNullElse(getenv("TIMEZONE"), ZoneId.systemDefault().getId()));

        var scoringSystemMetaInformation = MetaInformation.readFromPropertyStream(
                Main.class.getClassLoader().getResourceAsStream("app.properties"));


        var workflowTriggerQueue = new LinkedBlockingQueue<WorkflowTrigger>();
        var workflowResultQueue = new LinkedBlockingQueue<Message<String, ResultMessage>>();
        var workflowManager = new Thread(new WorkflowManager(
                workflowTriggerQueue,
                workflowResultQueue,
                WorkflowFactory.createAkiWorkflow(
                        scoringSystemMetaInformation.getProperty(MetaInformationProperty.NAME)
                                .orElse("AKI"),
                        scoringSystemMetaInformation.getProperty(MetaInformationProperty.VERSION)
                                .orElse("latest"),
                        timezone
                ),
                WorkflowScheduler.createVirtualWorkflowScheduler()
        ));

        var dbConnSettings = new DBConnectionSettings(mongoDBHost, mongoDBPort, mongoDBName);
        var mongoClient = RepositoryFactory.getMongoClient(dbConnSettings.getConnectionString());
        var mongoDatabase = RepositoryFactory.getMongoDatabase(dbConnSettings.getConnectionString());

        var patientRepo = RepositoryFactory.getInitializedPatientRepo(mongoDatabase);
        var medicalCaseRepo = RepositoryFactory.getInitializedMedicalCaseRepo(mongoDatabase);
        var labEvaluationRepo = RepositoryFactory.getInitializedLaboratoryEvaluationRepo(mongoDatabase);
        var procedureRepo = RepositoryFactory.getInitializedProcedureRepo(mongoDatabase);

        var workflowCleaner = new WorkflowCleaner(mongoClient, labEvaluationRepo, procedureRepo);
        var workflowTriggerListener = new WorkflowTriggerListener(medicalCaseRepo, patientRepo, labEvaluationRepo,
                procedureRepo, workflowCleaner, workflowTriggerQueue);

        var messagePersistenceHandler = new MessagePersistenceHandler(
                mongoClient,
                medicalCaseRepo,
                patientRepo,
                labEvaluationRepo,
                procedureRepo,
                messageBufferSize,
                persistenceFlushTimeoutMs
        );
        messagePersistenceHandler.registerLabEvaluationPersistedProspect(
                workflowTriggerListener::onLaboratoryEvaluationMessagesPersisted);

        var patientMessageHandler = new PatientMessageHandler(messagePersistenceHandler);
        var patientKafkaConsumer = getKafkaConsumer(kafkaBootstrapServers, KafkaTopics.PATIENTS, PatientMessage.class,
                kafkaConsumerGroupId);
        var patientTopicReader = getTopicReader(KafkaTopics.PATIENTS, patientKafkaConsumer, patientMessageHandler,
                kafkaConsumerCommitTimeoutMs);

        var medicalCaseMessageHandler = new MedicalCaseMessageHandler(messagePersistenceHandler);
        var medicalCaseKafkaConsumer = getKafkaConsumer(kafkaBootstrapServers, KafkaTopics.MEDICAL_CASES,
                MedCaseMessage.class, kafkaConsumerGroupId);
        var medicalCaseTopicReader = getTopicReader(KafkaTopics.MEDICAL_CASES, medicalCaseKafkaConsumer,
                medicalCaseMessageHandler, kafkaConsumerCommitTimeoutMs);

        var labMessageHandler = new LaboratoryMessageHandler(messagePersistenceHandler);
        var labKafkaConsumer = getKafkaConsumer(kafkaBootstrapServers, KafkaTopics.LABORATORY, LaboratoryMessage.class,
                kafkaConsumerGroupId);
        var labTopicReader = getTopicReader(KafkaTopics.LABORATORY, labKafkaConsumer, labMessageHandler,
                kafkaConsumerCommitTimeoutMs);

        var procedureMessageHandler = new ProcedureMessageHandler(messagePersistenceHandler);
        var procedureKafkaConsumer = getKafkaConsumer(kafkaBootstrapServers, KafkaTopics.PROCEDURES,
                ProcedureMessage.class, kafkaConsumerGroupId);
        var procedureTopicReader = getTopicReader(KafkaTopics.PROCEDURES, procedureKafkaConsumer,
                procedureMessageHandler, kafkaConsumerCommitTimeoutMs);

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

    private static <T> Thread getTopicReader(KafkaTopics topic,
                                             ThreadSafeKafkaConsumer<String, T> kafkaConsumer,
                                             MessageHandler<T> messageHandler,
                                             long commitTimeoutMs) {
        var reader = KafkaReader.<String, T>builder()
                .forTopic(topic)
                .withKafkaConsumer(kafkaConsumer)
                .withMessageHandler(kMsg -> messageHandler.handleMessage(kMsg.message()))
                .withOffsetCommitStrategy(new ImmediateOffsetCommitStrategy<>(kafkaConsumer, commitTimeoutMs))
                .build();

        return new Thread(reader);
    }

    private static <T> ThreadSafeKafkaConsumer<String, T> getKafkaConsumer(String bootstrapServers,
                                                                           KafkaTopics topic,
                                                                           Class<T> messageType,
                                                                           String consumerGroupId) {
        return ThreadSafeKafkaConsumer.<String, T>builder()
                .useBootstrapServers(bootstrapServers)
                .forTopic(topic)
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(messageType)))
                .withKafkaConsumerConfig(
                        KafkaConsumerConfig.builder()
                                .setConfigItem(GROUP_ID, consumerGroupId)
                                .setConfigItem(AUTO_OFFSET_RESET, "earliest")
                                .build())
                .build();
    }
}
