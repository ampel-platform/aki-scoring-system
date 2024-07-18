package de.uni_leipzig.mds;

import de.uni_leipzig.mds.data.db.IAKIRepo;
import de.uni_leipzig.mds.evaluation.workflow.WorkflowPipeline;
import de.uni_leipzig.mds.io.connection.KafkaTopic;
import de.uni_leipzig.mds.io.connection.serialization.types.LaboratoryValueMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.ProcedureMessage;
import io.vavr.Tuple2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.mongodb.assertions.Assertions.assertFalse;
import static com.mongodb.assertions.Assertions.assertTrue;
import static de.uni_leipzig.mds.data.db.Codes.LAB_CODES_CREATININE;
import static de.uni_leipzig.mds.data.db.Codes.PROCEDURE_CODES_DIALYSIS;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static IAKIRepo mockedAkiRepo;

    @BeforeAll
    public static void setUp() {
        mockedAkiRepo = Mockito.mock(IAKIRepo.class);
    }

    // TODO: use parameterized tests
    @Test
    public void testLabMessagesOlderThan8DaysAreDropped() throws InterruptedException {
        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopic, Object>>();
        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();

        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
        var procThread = new Thread(proc);
        procThread.start();

        var expiredLaboratoryMessage = new LaboratoryValueMessage.Builder()
                .withPatientId("123456789")
                .withCaseId("test-case-id")
                .withOrderId("ABC")
                .withAnalyteCode(LAB_CODES_CREATININE.getFirst())
                .withAnalyteValue(20.0)
                .withMeasurementTime(LocalDateTime.now().minusDays(9))
                .build();

        var latch = new CountDownLatch(1);
        Mockito.doAnswer((x) -> {
            latch.countDown();
            return null;
        }).when(mockedAkiRepo).insertLabValue(any());

        incomingMessages.put(new Tuple2<>(KafkaTopic.LABORATORY_VALUES, expiredLaboratoryMessage));

        // Actually resembles a Mockito.versify checking that a function on a mocked object has not been invoked.
        // However, this is done with concurrency in mind as well as without any use of sleep functionality that would
        // be brittle.
        assertFalse(latch.await(500L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testLabMessagesYoungerThan8DaysAreKept() throws InterruptedException {
        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopic, Object>>();
        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();

        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
        var procThread = new Thread(proc);
        procThread.start();

        var unexpiredLaboratoryMessage = new LaboratoryValueMessage.Builder()
                .withPatientId("123456789")
                .withCaseId("test-case-id")
                .withOrderId("DEF")
                .withAnalyteCode(LAB_CODES_CREATININE.getFirst())
                .withAnalyteValue(15.0)
                .withMeasurementTime(LocalDateTime.now())
                .build();

        var latch = new CountDownLatch(1);
        Mockito.doAnswer((x) -> {
            latch.countDown();
            return null;
        }).when(mockedAkiRepo).insertLabValue(any());

        incomingMessages.put(new Tuple2<>(KafkaTopic.LABORATORY_VALUES, unexpiredLaboratoryMessage));

        // Actually resembles a Mockito.verify without the need of using any sleep functionality that would be brittle.
        assertTrue(latch.await(1L, TimeUnit.MINUTES));
    }

    @Test
    public void testProcedureMessagesOlderThan8DaysAreDropped() throws InterruptedException {
        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopic, Object>>();
        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();

        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
        var procThread = new Thread(proc);
        procThread.start();

        var expiredProcedureMessage = new ProcedureMessage.Builder()
                .withPatientId("123456789")
                .withCaseId("test-case-id")
                .withProcedureCode(PROCEDURE_CODES_DIALYSIS.getFirst())
                .withPerformedOn(LocalDateTime.now().minusDays(9))
                .build();

        var latch = new CountDownLatch(1);
        Mockito.doAnswer((x) -> {
            latch.countDown();
            return null;
        }).when(mockedAkiRepo).insertProcedure(any());

        incomingMessages.put(new Tuple2<>(KafkaTopic.PROCEDURES, expiredProcedureMessage));

        // Actually resembles a Mockito.versify checking that a function on a mocked object has not been invoked.
        // However, this is done with concurrency in mind as well as without any use of sleep functionality that would
        // be brittle.
        assertFalse(latch.await(500L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testProcedureMessagesYoungerThan8DaysAreKept() throws InterruptedException {
        var incomingMessages = new LinkedBlockingQueue<Tuple2<KafkaTopic, Object>>();
        var workflowPipelineInputQueue = new LinkedBlockingQueue<WorkflowPipeline.WorkflowPipelineInput>();

        var proc = new MessageProcessor(incomingMessages, workflowPipelineInputQueue, mockedAkiRepo);
        var procThread = new Thread(proc);
        procThread.start();

        var unexpiredProcedureMessage = new ProcedureMessage.Builder()
                .withPatientId("123456789")
                .withCaseId("test-case-id")
                .withProcedureCode(PROCEDURE_CODES_DIALYSIS.getFirst())
                .withPerformedOn(LocalDateTime.now())
                .build();

        var latch = new CountDownLatch(1);
        Mockito.doAnswer((x) -> {
            latch.countDown();
            return null;
        }).when(mockedAkiRepo).insertProcedure(any());

        incomingMessages.put(new Tuple2<>(KafkaTopic.PROCEDURES, unexpiredProcedureMessage));

        // Actually resembles a Mockito.verify without the need of using any sleep functionality that would be brittle.
        assertTrue(latch.await(1L, TimeUnit.MINUTES));
    }
}
