import csv.CsvToBeanFactory;
import csv.lab.LaboratoryData;
import csv.medcase.MedicalCaseData;
import csv.output.OutputData;
import csv.patient.PatientData;
import csv.procedure.ProcedureData;
import de.uni_leipzig.ampel.sdk.io.kafka.*;
import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.MessageHeader;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.ampel.sdk.io.message.result.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import util.PreProcessingUtil;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        var kafkaBootstrapServers = requireNonNull(getenv("KAFKA_BOOTSTRAP_SERVERS"), "kafka bootstrap servers must be specified");
        var patientDataSource = requireNonNull(getenv("PATIENT_DATA_SOURCE"), "patient data source must be specified");
        var medCaseDataSource = requireNonNull(getenv("MED_CASE_DATA_SOURCE"), "medical case data source must be specified");
        var procedureDataSource = requireNonNull(getenv("PROCEDURE_DATA_SOURCE"), "procedure data source must be specified");
        var labDataSource = requireNonNull(getenv("LABORATORY_DATA_SOURCE"), "lab data source must be specified");
        var expectedResultsSource = requireNonNull(getenv("EXPECTED_RESULTS_SOURCE"), "expected results data source must be specified");

        var patients = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(patientDataSource),
                PatientData.class).parse();
        var medicalCases = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(medCaseDataSource),
                MedicalCaseData.class).parse();
        var procedures = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(procedureDataSource),
                ProcedureData.class).parse();
        var labData = PreProcessingUtil.filterForNonNaNValues(CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(labDataSource),
                LaboratoryData.class).parse());
        var expectedResults = CsvToBeanFactory.getCsvToBeanTransformer(new FileReader(expectedResultsSource),
                OutputData.class).parse();

        var timeBasedUploadFunctions = new HashMap<LocalDateTime, Callable<Void>>(); // used for sorting

        // PATIENTS
        var patientMessageQueue = new LinkedBlockingQueue<Message<String, PatientMessage>>();
        patients.forEach(patient -> timeBasedUploadFunctions.put(
                patient.getCreationDate().atStartOfDay(),
                () -> {
                    log.info("uploading patient: {} - {}", patient.getId(), patient.getCreationDate().atStartOfDay());
                    patientMessageQueue.put(new Message<>(
                            patient.getId(),
                            new MessageHeader(
                                    patient.getCreationDate().atStartOfDay(),
                                    "v1.0.0-validation",
                                    "validation-test"
                            ),
                            patient.convertToPatientMessage()
                    ));
                    return null;
                }));


        // MEDICAL CASES
        var medicalCaseMessageQueue = new LinkedBlockingQueue<Message<String, MedCaseMessage>>();
        medicalCases.forEach(medicalCase -> timeBasedUploadFunctions.put(
                medicalCase.getCreationDate().atStartOfDay(),
                () -> {
                    log.info("uploading medical case: {} - {}", medicalCase.getId(), medicalCase.getCreationDate().atStartOfDay());
                    medicalCaseMessageQueue.put(new Message<>(
                            medicalCase.getId(),
                            new MessageHeader(
                                    medicalCase.getCreationDate().atStartOfDay(),
                                    "v1.0.0-validation",
                                    "validation-test"
                            ),
                            medicalCase.convertToMedicalCaseMessage()
                    ));
                    return null;
                }));


        // PROCEDURES
        var procedureMessageQueue = new LinkedBlockingQueue<Message<String, ProcedureMessage>>();
        procedures.forEach(procedure -> timeBasedUploadFunctions.put(
                procedure.getPerformedOn(),
                () -> {
                    log.info("uploading procedure: {} - {}", procedure.getId(), procedure.getPerformedOn());
                    procedureMessageQueue.put(new Message<>(
                            procedure.getMedicalCaseId(),
                            new MessageHeader(
                                    procedure.getPerformedOn(),
                                    "v1.0.0-validation",
                                    "validation-test"
                            ),
                            procedure.convertToProcedureMessage()
                    ));
                    return null;
                }
        ));


        // LABORATORY
        var laboratoryMessageQueue = new LinkedBlockingQueue<Message<String, LaboratoryMessage>>();
        labData.forEach(lab -> timeBasedUploadFunctions.put(
                lab.getMeasuredAt(),
                () -> {
                    log.info("uploading laboratory event: {} - {}", lab.getDocumentId(), lab.getMeasuredAt());
                    laboratoryMessageQueue.put(new Message<>(
                            lab.getMedicalCaseId(),
                            new MessageHeader(
                                    lab.getCreatedAt(),
                                    "v1.0.0-validation",
                                    "validation-test"
                            ),
                            lab.convertToLaboratoryMessage()
                    ));
                    return null;
                }
        ));


        var patientPublisher = createKafkaWriter(kafkaBootstrapServers, KafkaTopics.PATIENTS, patientMessageQueue,
                PatientMessage.class);

        var medicalCasePublisher = createKafkaWriter(kafkaBootstrapServers, KafkaTopics.MEDICAL_CASES,
                medicalCaseMessageQueue, MedCaseMessage.class);

        var procedurePublisher = createKafkaWriter(kafkaBootstrapServers, KafkaTopics.PROCEDURES, procedureMessageQueue,
                ProcedureMessage.class);

        var laboratoryPublisher = createKafkaWriter(kafkaBootstrapServers, KafkaTopics.LABORATORY,
                laboratoryMessageQueue, LaboratoryMessage.class);

        log.info("starting patient message publisher");
        new Thread(patientPublisher).start();
        log.info("starting medical case message publisher");
        new Thread(medicalCasePublisher).start();
        log.info("starting procedure message publisher");
        new Thread(procedurePublisher).start();
        log.info("starting laboratory message publisher");
        new Thread(laboratoryPublisher).start();

        // Sort
        var eventOccurrencesDesc = timeBasedUploadFunctions.keySet().stream().sorted().toList();

        // Execute
        for (LocalDateTime time : eventOccurrencesDesc) {
            try {
                timeBasedUploadFunctions.get(time)
                        .call();
                // wait until all queues are empty
                log.info("waiting for message queues to get drained");
                waitUntilMessageQueueIsDrained(patientMessageQueue);
                waitUntilMessageQueueIsDrained(medicalCaseMessageQueue);
                waitUntilMessageQueueIsDrained(procedureMessageQueue);
                waitUntilMessageQueueIsDrained(laboratoryMessageQueue);
            } catch (Exception e) {
                log.error("could not upload resource for timestamp {}", time, e);
            }
        }

        log.info("waiting for all message queues to be drained");
        try {
            waitUntilMessageQueueIsDrained(patientMessageQueue);
            log.info("patient message queue drained");
            waitUntilMessageQueueIsDrained(medicalCaseMessageQueue);
            log.info("medical case message queue drained");
            waitUntilMessageQueueIsDrained(procedureMessageQueue);
            log.info("procedure message queue drained");
            waitUntilMessageQueueIsDrained(laboratoryMessageQueue);
            log.info("laboratory message queue drained");
        } catch (Exception e) {
            log.error("failed waiting for message queues to be drained", e);
        }

        patientPublisher.shutdown();
        medicalCasePublisher.shutdown();
        procedurePublisher.shutdown();
        laboratoryPublisher.shutdown();


        // READING BACK RESULTS
        var resultReaderCfg = KafkaReaderConfig.builder()
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.GROUP_ID, "aki-validation-result")
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.ENABLE_AUTO_COMMIT, false)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, 5000L)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                .build();

        var timer = new Timer();
        var latch = new CountDownLatch(1);
        TimerTask timerTask = createCountdownTask(latch);

        List<Message<String, ResultMessage>> resultMessages = new ArrayList<>();
        var resultReader = KafkaReader.<String, ResultMessage>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .forTopic(KafkaTopics.AMPEL_RESULT)
                .withKafkaReaderConfig(resultReaderCfg)
                .withDeserializationStrategy(
                        new KafkaDeserializationStrategy<>(
                                new StringDeserializer(),
                                KafkaSerde.createKafkaDeserializer(ResultMessage.class)))
                .withMessageConsumer(msg -> {
                    timerTask.cancel();
                    timer.purge();
                    resultMessages.add(msg);
                    timer.schedule(createCountdownTask(latch), 10000);
                })
                .build();

        log.info("starting result message reader");
        new Thread(resultReader).start();

        // wait until there's no more incoming result messages
        latch.await();

        log.info("stopping result reader");
        resultReader.shutdown();


        var categories = new HashMap<String, Integer>();
        for (Message<String, ResultMessage> resultMessage : resultMessages) {
            var severity = resultMessage.body().severityRepresentation();
            var occurrences = categories.getOrDefault(severity, 0);

            categories.put(severity, occurrences + 1);
        }

        for (Map.Entry<String, Integer> cats : categories.entrySet()) {
            log.info("category {} - #entries: {}", cats.getKey(), cats.getValue());
        }
    }

    private static TimerTask createCountdownTask(CountDownLatch latch) {
        return new TimerTask() {
            @Override
            public void run() {
                latch.countDown();
            }
        };
    }

    private static <T> KafkaWriter<String, T> createKafkaWriter(String kafkaBootstrapServers,
                                                                KafkaTopics topic,
                                                                BlockingQueue<Message<String, T>> messageQueue,
                                                                Class<T> messageClass) {
        return KafkaWriter.<String, T>builder()
                .useBootstrapServers(kafkaBootstrapServers)
                .useTopic(topic)
                .withMessagePublishingChannel(messageQueue)
                .withSerializationStrategy(
                        new KafkaSerializationStrategy<>(
                                new StringSerializer(),
                                KafkaSerde.createKafkaSerializer(messageClass)))
                .withKafkaWriterConfig(
                        KafkaWriterConfig.builder()
                                .setConfigItem(KafkaWriterConfig.KafkaWriterConfigItem.CLIENT_ID, "aki-validation")
                                .build())
                .build();
    }

    private static void waitUntilMessageQueueIsDrained(BlockingQueue<?> messageQueue) throws InterruptedException {
        while (!messageQueue.isEmpty()) {
            // hot loop but good enough for validation purposes
            Thread.sleep(50L);
        }
    }
}
