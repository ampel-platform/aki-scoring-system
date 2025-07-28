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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import util.PreProcessingUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static java.lang.System.getenv;
import static java.util.Objects.requireNonNull;

@Slf4j
public class Main {

    public static void main(String[] args) throws InterruptedException {
        var kafkaBootstrapServers = requireNonNull(getenv("KAFKA_BOOTSTRAP_SERVERS"), "kafka bootstrap servers must be specified");
        var patients = CsvToBeanFactory.getCsvToBeanTransformer(
                        readDataOrUseBundledTestdata(getenv("PATIENT_DATA_SOURCE"), "testdata/patients.csv"),
                        PatientData.class)
                .parse();
        var medicalCases = CsvToBeanFactory.getCsvToBeanTransformer(
                        readDataOrUseBundledTestdata(getenv("MED_CASE_DATA_SOURCE"), "testdata/medical-cases.csv"),
                        MedicalCaseData.class)
                .parse();
        var procedures = CsvToBeanFactory.getCsvToBeanTransformer(
                        readDataOrUseBundledTestdata(getenv("PROCEDURE_DATA_SOURCE"), "testdata/procedures.csv"),
                        ProcedureData.class)
                .parse();
        var labEvaluationData = CsvToBeanFactory.getCsvToBeanTransformer(
                        readDataOrUseBundledTestdata(getenv("LABORATORY_DATA_SOURCE"), "testdata/laboratory-evaluations.csv"),
                        LaboratoryData.class)
                .parse();
        var expectedResults = CsvToBeanFactory.getCsvToBeanTransformer(
                        readDataOrUseBundledTestdata(getenv("EXPECTED_RESULTS_SOURCE"), "testdata/expected-results.csv"),
                        OutputData.class)
                .parse();

        var labEntriesWithNaNValues = PreProcessingUtil.getLabEntriesWithNaNValues(labEvaluationData);
        labEvaluationData.removeAll(labEntriesWithNaNValues);

        // Using this as a sort of multimap due to the fact that some timestamps may occur several times
        var timeBasedUploadFunctions = new HashMap<LocalDateTime, List<Callable<Void>>>(); // used for sorting

        // PATIENTS
        var patientMessageQueue = new LinkedBlockingQueue<Message<String, PatientMessage>>();
        patients.forEach(patient -> {
            var potentiallyExistingCallables = timeBasedUploadFunctions.getOrDefault(patient.getCreationDate()
                    .atStartOfDay(), new ArrayList<>());

            potentiallyExistingCallables.add(() -> {
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
            });

            timeBasedUploadFunctions.put(patient.getCreationDate().atStartOfDay(), potentiallyExistingCallables);
        });


        // MEDICAL CASES
        var medicalCaseMessageQueue = new LinkedBlockingQueue<Message<String, MedCaseMessage>>();
        medicalCases.forEach(medicalCase -> {
            var potentiallyExistingCallables = timeBasedUploadFunctions.getOrDefault(medicalCase.getCreationDate()
                    .atStartOfDay(), new ArrayList<>());

            potentiallyExistingCallables.add(() -> {
                log.info("uploading medical case: {} - {}", medicalCase.getId(), medicalCase.getCreationDate()
                        .atStartOfDay());
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
            });

            timeBasedUploadFunctions.put(medicalCase.getCreationDate().atStartOfDay(), potentiallyExistingCallables);
        });


        // PROCEDURES
        var procedureMessageQueue = new LinkedBlockingQueue<Message<String, ProcedureMessage>>();
        procedures.forEach(procedure -> {
            var potentiallyExistingCallables = timeBasedUploadFunctions.getOrDefault(procedure.getPerformedOn(),
                    new ArrayList<>());

            potentiallyExistingCallables.add(() -> {
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
            );

            timeBasedUploadFunctions.put(procedure.getPerformedOn(), potentiallyExistingCallables);
        });


        // LABORATORY
        var laboratoryMessageQueue = new LinkedBlockingQueue<Message<String, LaboratoryMessage>>();
        labEvaluationData.forEach(lab -> {
            var potentiallyExistingCallables = timeBasedUploadFunctions.getOrDefault(lab.getMeasuredAt(),
                    new ArrayList<>());

            potentiallyExistingCallables.add(() -> {
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
            );

            timeBasedUploadFunctions.put(lab.getMeasuredAt(), potentiallyExistingCallables);
        });


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
            var uploadFns = timeBasedUploadFunctions.get(time);
            for (Callable<Void> uploadFn : uploadFns) {
                try {
                    uploadFn.call();
                    // wait until all queues are empty
                    log.debug("waiting for message queues to get drained");
                    waitUntilMessageQueueIsDrained(patientMessageQueue);
                    waitUntilMessageQueueIsDrained(medicalCaseMessageQueue);
                    waitUntilMessageQueueIsDrained(procedureMessageQueue);
                    waitUntilMessageQueueIsDrained(laboratoryMessageQueue);
                } catch (Exception e) {
                    log.error("could not upload resource for timestamp {}", time, e);
                }
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
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.COMMIT_TIMEOUT_MS, 5000L)
                .setConfigItem(KafkaReaderConfig.KafkaReaderConfigItem.AUTO_OFFSET_RESET, "earliest")
                .build();

        var timer = new Timer();
        var latch = new CountDownLatch(1);
        TimerTask timerTask = createCountdownTask(latch);

        List<ResultMessage> resultMessages = new ArrayList<>();
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
                    resultMessages.add(msg.body());
                    timer.schedule(createCountdownTask(latch), 10000);
                })
                .build();

        log.info("starting result message reader");
        new Thread(resultReader).start();

        // wait until there's no more incoming result messages
        latch.await();
        timer.cancel();

        log.info("stopping result reader");
        resultReader.shutdown();


        var categories = new HashMap<String, Integer>();
        for (ResultMessage resultMessage : resultMessages) {
            var severity = resultMessage.severityRepresentation();
            var occurrences = categories.getOrDefault(severity, 0);

            categories.put(severity, occurrences + 1);
        }

        for (Map.Entry<String, Integer> cats : categories.entrySet()) {
            log.info("category {} - #entries: {}", cats.getKey(), cats.getValue());
        }

        // categorize expectedResults
        var relevantExpectedResults = PreProcessingUtil.filterResultsForOrdersOfExistingLabEntries(expectedResults,
                labEvaluationData);
        log.info("relevant expected results: #{}", relevantExpectedResults.size());
        var orderIdResultMapping = relevantExpectedResults.stream()
                .collect(
                        Collectors.toMap(OutputData::getOrderId,
                                r -> r,
                                (prev, next) -> next,
                                HashMap::new));
        log.info("remaining expected results to be checked: #{}", orderIdResultMapping.size());

        for (ResultMessage res : resultMessages) {
            var resultOrderId = res.meta().get("workflowSummary").get("order_id").asText();
            log.info("validating result for orderId: '{}'", resultOrderId);

            var resSeverity = res.severityRepresentation() == null
                    ? ""
                    : res.severityRepresentation();

            var expectedResult = orderIdResultMapping.remove(resultOrderId);
            if (expectedResult == null) {
                log.error("result for order '{}' is not part of the set of expected results", resultOrderId);
                System.exit(1);
                continue;
            }

            if (!Objects.equals(expectedResult.getMedicalCaseId(), res.medicalCaseId())
                    || !Objects.equals(expectedResult.getSeverityRepresentation(), resSeverity)) {
                log.error("results do not match | expected case id: '{}' got '{}' | expected severity '{}' got '{}'",
                        expectedResult.getMedicalCaseId(), res.medicalCaseId(),
                        expectedResult.getSeverityRepresentation(), resSeverity);
                System.exit(1);
            }
        }

        // what's left
        if (!orderIdResultMapping.isEmpty()) {
            log.warn("there are #{} entries left in the expected results that do not have a matching registered result",
                    orderIdResultMapping.size());

            for (OutputData remainingExpectedResult : orderIdResultMapping.values()) {
                log.warn("expected result '{}' for order '{}' but got nothing",
                        remainingExpectedResult.getSeverityRepresentation(), remainingExpectedResult.getOrderId());
            }
            System.exit(1);
        }

        System.exit(0);
    }

    private static InputStreamReader readDataOrUseBundledTestdata(
            String path,
            @NonNull String bundledFallbackResourcePath) {
        return Optional.ofNullable(path)
                .map(s -> {
                    try {
                        return (InputStreamReader) new FileReader(s);
                    } catch (FileNotFoundException e) {
                        log.error("could not open file {}", s, e);
                        System.exit(1);
                    }
                    return null;
                })
                .orElseGet(() -> {
                    var bundledResource = Main.class.getClassLoader().getResourceAsStream(bundledFallbackResourcePath);
                    if (bundledResource == null) {
                        log.error("could not locate bundled resource at {}", bundledFallbackResourcePath);
                        System.exit(1);
                    }
                    return new InputStreamReader(bundledResource);
                });
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
