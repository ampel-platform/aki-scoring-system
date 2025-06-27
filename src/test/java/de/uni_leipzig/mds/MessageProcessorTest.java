//package de.uni_leipzig.mds;
//
//import de.uni_leipzig.mds.data.db.IAKIRepo;
//import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline;
//import de.uni_leipzig.mds.io.kafka.KafkaTopics;
//import de.uni_leipzig.mds.io.message.laboratory.LaboratoryMessage;
//import de.uni_leipzig.mds.io.message.procedure.ProcedureMessage;
//import de.uni_leipzig.mds.processing.MessageProcessor;
//import io.vavr.Tuple2;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//
//import static com.mongodb.assertions.Assertions.assertFalse;
//import static com.mongodb.assertions.Assertions.assertTrue;
//import static de.uni_leipzig.mds.processing.Codes.LAB_CODES_CREATININE;
//import static de.uni_leipzig.mds.processing.Codes.PROCEDURE_CODES_DIALYSIS;
//import static org.mockito.ArgumentMatchers.any;
//
//@ExtendWith(MockitoExtension.class)
//class MessageProcessorTest {
//
//    private static IAKIRepo mockedAkiRepo;
//
//    @BeforeAll
//    public static void setUp() {
//        mockedAkiRepo = Mockito.mock(IAKIRepo.class);
//    }
//
//    // TODO: use parameterized tests
//    @Test
//    public void testLabMessagesOlderThan8DaysAreDropped() throws InterruptedException {
//        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Object>>();
//        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
//
//        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
//        var procThread = new Thread(proc);
//        procThread.start();
//
//        var expiredLaboratoryMessage = new LaboratoryMessage(
//                "doc-123",
//                "v1",
//                "p1",
//                "a",
//                "test-case-id",
//                "123456789",
//                "ord-123",
//                LocalDateTime.now().minusDays(4),
//                "orig-system",
//                LAB_CODES_CREATININE.getFirst(),
//                20.0,
//                "µmol/l",
//                LocalDateTime.now().minusDays(9)
//        );
//
//        var latch = new CountDownLatch(1);
//        Mockito.doAnswer((x) -> {
//            latch.countDown();
//            return null;
//        }).when(mockedAkiRepo).insertLabValue(any());
//
//        incomingMessages.put(new Tuple2<>(KafkaTopics.LABORATORY, expiredLaboratoryMessage));
//
//        // Actually resembles a Mockito.versify checking that a function on a mocked object has not been invoked.
//        // However, this is done with concurrency in mind as well as without any use of sleep functionality that would
//        // be brittle.
//        assertFalse(latch.await(500L, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testLabMessagesYoungerThan8DaysAreKept() throws InterruptedException {
//        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Object>>();
//        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
//
//        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
//        var procThread = new Thread(proc);
//        procThread.start();
//
//        var unexpiredLaboratoryMessage = new LaboratoryMessage(
//                "doc-123",
//                "v1",
//                "p1",
//                "a",
//                "test-case-id",
//                "123456789",
//                "ord-123",
//                LocalDateTime.now(),
//                "orig-system",
//                LAB_CODES_CREATININE.getFirst(),
//                20.0,
//                "µmol/l",
//                LocalDateTime.now()
//        );
//
//        var latch = new CountDownLatch(1);
//        Mockito.doAnswer((x) -> {
//            latch.countDown();
//            return null;
//        }).when(mockedAkiRepo).insertLabValue(any());
//
//        incomingMessages.put(new Tuple2<>(KafkaTopics.LABORATORY, unexpiredLaboratoryMessage));
//
//        // Actually resembles a Mockito.verify without the need of using any sleep functionality that would be brittle.
//        assertTrue(latch.await(1L, TimeUnit.MINUTES));
//    }
//
//    @Test
//    public void testProcedureMessagesOlderThan8DaysAreDropped() throws InterruptedException {
//        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Object>>();
//        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
//
//        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
//        var procThread = new Thread(proc);
//        procThread.start();
//
//        var expiredProcedureMessage = new ProcedureMessage(
//                "proc-1234",
//                "test-case-id",
//                PROCEDURE_CODES_DIALYSIS.getFirst(),
//                LocalDateTime.now().minusDays(9),
//                LocalDate.of(100, 1, 1),
//                LocalDate.of(101, 1, 1)
//        );
//
//        var latch = new CountDownLatch(1);
//        Mockito.doAnswer((x) -> {
//            latch.countDown();
//            return null;
//        }).when(mockedAkiRepo).insertProcedure(any());
//
//        incomingMessages.put(new Tuple2<>(KafkaTopics.PROCEDURES, expiredProcedureMessage));
//
//        // Actually resembles a Mockito.versify checking that a function on a mocked object has not been invoked.
//        // However, this is done with concurrency in mind as well as without any use of sleep functionality that would
//        // be brittle.
//        assertFalse(latch.await(500L, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testProcedureMessagesYoungerThan8DaysAreKept() throws InterruptedException {
//        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopics, Object>>();
//        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();
//
//        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
//        var procThread = new Thread(proc);
//        procThread.start();
//
//        var unexpiredProcedureMessage = new ProcedureMessage(
//                "proc-1234",
//                "test-case-id",
//                PROCEDURE_CODES_DIALYSIS.getFirst(),
//                LocalDateTime.now(),
//                LocalDate.of(100, 1, 1),
//                LocalDate.of(101, 1, 1)
//        );
//
//        var latch = new CountDownLatch(1);
//        Mockito.doAnswer((x) -> {
//            latch.countDown();
//            return null;
//        }).when(mockedAkiRepo).insertProcedure(any());
//
//        incomingMessages.put(new Tuple2<>(KafkaTopics.PROCEDURES, unexpiredProcedureMessage));
//
//        // Actually resembles a Mockito.verify without the need of using any sleep functionality that would be brittle.
//        assertTrue(latch.await(1L, TimeUnit.MINUTES));
//    }
//}
