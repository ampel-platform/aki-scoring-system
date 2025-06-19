//package de.uni_leipzig.mds.data.db;
//
//import com.mongodb.client.MongoClient;
//import de.uni_leipzig.mds.db.ClientFactory;
//import de.uni_leipzig.mds.db.DBConnectionSettings;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.utility.DockerImageName;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static com.mongodb.assertions.Assertions.assertFalse;
//import static com.mongodb.assertions.Assertions.assertTrue;
//import static de.uni_leipzig.mds.processing.Codes.*;
//import static java.time.temporal.ChronoUnit.DAYS;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class RepoFactoryIT {
//
//    private static final String DATEBASE_NAME = "test";
//    private MongoClient mongoDbClient;
//    private AKIRepo akiRepo;
//
//    @Container
//    public GenericContainer<?> mongoDB = new GenericContainer(DockerImageName.parse("mongo:7.0.5-jammy@sha256:c1857c629fd8c5ea094e2a076e8f123704ae8604d33df3b6ef7573f8d0c44163"))
//            .withExposedPorts(27017)
//            .withEnv("MONGO_INITDB_DATABASE", DATEBASE_NAME)
//            .waitingFor(
//                    Wait.forSuccessfulCommand("echo 'db.runCommand(\"ping\").ok' | mongosh localhost:27017/%s --quiet"
//                            .formatted(DATEBASE_NAME)));
//
//    @BeforeEach
//    public void setUp() {
//        mongoDB.start();
//
//        var connSettings = new DBConnectionSettings(mongoDB.getHost(), mongoDB.getFirstMappedPort().toString(), DATEBASE_NAME);
//        mongoDbClient = ClientFactory.createDBClient(connSettings.getConnectionString());
//        var repoFactory = new RepoFactory(mongoDbClient.getDatabase(DATEBASE_NAME));
//        akiRepo =  repoFactory.createAkiRepo();
//    }
//
//    @AfterEach
//    public void tearDown() {
//        mongoDB.stop();
//        mongoDbClient.close();
//    }
//
//
//    @Nested
//    class AkiRepoTests {
//
//        @Test
//        public void returnsNoFeedIfPatientDoesNotExist() {
//            assertTrue(akiRepo.findPatientFeed("unknown", "unknown").isEmpty());
//        }
//
//        @Test
//        public void returnsFeedIfPatientExists() {
//            var patientDateOfBirth = LocalDate.of(1950, 1, 1);
//            akiRepo.insertPatient(Patient.Builder.builder()
//                    .patientRef("123456789")
//                    .dateOfBirth(patientDateOfBirth)
//                    .deceased(false)
//                    .build());
//
//            var patientFeed = akiRepo.findPatientFeed("123456789", "unknown");
//            assertTrue(patientFeed.isPresent());
//            assertEquals("123456789", patientFeed.get().getPatientId());
//            assertEquals("unknown", patientFeed.get().getCaseId());
//            assertEquals(patientDateOfBirth, patientFeed.get().getDateOfBirth());
//            assertFalse(patientFeed.get().isDeceased());
//            assertTrue(patientFeed.get().getCreatinineValues().isEmpty());
//            assertTrue(patientFeed.get().getVancomycinValues().isEmpty());
//            assertTrue(patientFeed.get().getDialysisProcedures().isEmpty());
//        }
//
//        @Test
//        public void insertedLabValuesOlderThan8DaysAreEventuallyDropped() throws InterruptedException {
//            akiRepo.insertPatient(Patient.Builder.builder()
//                    .patientRef("123456789")
//                    .dateOfBirth(LocalDate.of(1950, 1, 1))
//                    .deceased(false)
//                    .build()
//            );
//
//            akiRepo.insertLabValue(LaboratoryValue.Builder.builder()
//                    .code(LAB_CODES_CREATININE.getFirst())
//                    .value(20.0)
//                    .unit("mg/dL")
//                    .time(LocalDateTime.now().minusDays(9))
//                    .refs("ABC", "123456789", "test-case-id")
//                    .build());
//
//            akiRepo.insertLabValue(LaboratoryValue.Builder.builder()
//                    .code(LAB_CODES_CREATININE.getFirst())
//                    .value(15.7)
//                    .unit("mg/dL")
//                    .time(LocalDateTime.now().minusDays(2))
//                    .refs("ABC-2", "123456789", "test-case-id")
//                    .build());
//
//            akiRepo.insertLabValue(LaboratoryValue.Builder.builder()
//                    .code(LAB_CODES_VANCOMYCIN.getFirst())
//                    .value(10.07)
//                    .unit("")
//                    .time(LocalDateTime.now().minusDays(10))
//                    .refs("DEF", "123456789", "test-case-id")
//                    .build());
//
//            akiRepo.insertLabValue(LaboratoryValue.Builder.builder()
//                    .code(LAB_CODES_VANCOMYCIN.getFirst())
//                    .value(18.72)
//                    .unit("")
//                    .time(LocalDateTime.now().minusDays(1))
//                    .refs("DEF-2", "123456789", "test-case-id")
//                    .build());
//
//            var latch = new CountDownLatch(1);
//
//            new Thread(() -> {
//                while (true) {
//                    var potentialPatientFeed = akiRepo.findPatientFeed("123456789", "test-case-id");
//                    if (potentialPatientFeed.isPresent()) {
//                        var patientFeed = potentialPatientFeed.get();
//
//                        var containsUnexpiredCreatinineValue = !patientFeed.getCreatinineValues().stream()
//                                .takeWhile((v) -> v.getOrderIdRef().equals("ABC-2")).toList().isEmpty();
//                        var containsUnexpiredVancomycinValue = !patientFeed.getVancomycinValues().stream()
//                                .takeWhile((v) -> v.getOrderIdRef().equals("DEF-2")).toList().isEmpty();
//
//                        if (patientFeed.getCreatinineValues().size() == 1 && containsUnexpiredCreatinineValue
//                                && patientFeed.getVancomycinValues().size() == 1 && containsUnexpiredVancomycinValue) {
//                            latch.countDown();
//                            break;
//                        }
//                    }
//                }
//            }).start();
//
//            // MongoDB has a background task scheduled to run every 60 seconds. This task removes expired records
//            // (according to the TTL). In order to account for an unfortunate overlap, 2 minutes waiting time is used.
//            assertTrue(latch.await(2L, TimeUnit.MINUTES));
//        }
//
//        @Test
//        public void insertedProcedureValuesOlderThan8DaysAreEventuallyDropped() throws InterruptedException {
//            akiRepo.insertPatient(Patient.Builder.builder()
//                    .patientRef("123456789")
//                    .dateOfBirth(LocalDate.of(1950, 1, 1))
//                    .deceased(false)
//                    .build());
//
//            akiRepo.insertProcedure(Procedure.Builder.builder()
//                    .time(LocalDateTime.now().minusDays(9))
//                    .code(PROCEDURE_CODES_DIALYSIS.getFirst())
//                    .refs("123456789", "test-case-id")
//                    .build());
//
//            akiRepo.insertProcedure(Procedure.Builder.builder()
//                    .time(LocalDateTime.now().minusDays(4))
//                    .code(PROCEDURE_CODES_DIALYSIS.getFirst())
//                    .refs("123456789", "test-case-id")
//                    .build());
//
//            var latch = new CountDownLatch(1);
//
//            new Thread(() -> {
//                while (true) {
//                    var potentialPatientFeed = akiRepo.findPatientFeed("123456789", "test-case-id");
//                    if (potentialPatientFeed.isPresent()) {
//                        var patientFeed = potentialPatientFeed.get();
//
//                        var containsUnexpiredProcedures = !patientFeed.getDialysisProcedures().stream()
//                                .takeWhile((v) -> Math.abs(v.getTime().until(LocalDateTime.now(), DAYS)) < 8L)
//                                .toList().isEmpty();
//
//                        if (patientFeed.getDialysisProcedures().size() == 1 && containsUnexpiredProcedures) {
//                            latch.countDown();
//                            break;
//                        }
//                    }
//                }
//            }).start();
//
//            // MongoDB has a background task scheduled to run every 60 seconds. This task removes expired records
//            // (according to the TTL). In order to account for an unfortunate overlap, 2 minutes waiting time is used.
//            assertTrue(latch.await(2L, TimeUnit.MINUTES));
//        }
//    }
//}
