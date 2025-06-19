package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.evaluation.workflow.Workflow;
import de.uni_leipzig.ampel.sdk.evaluation.workflow.WorkflowScheduler;
import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.result.ResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

// TODO: add doc
public final class WorkflowManager implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowManager.class);

    private final BlockingQueue<WorkflowTrigger> workflowTriggerQueue;
    private final BlockingQueue<Message<String, ResultMessage>> workflowResultQueue;
    private final Workflow workflow;
    private final WorkflowScheduler workflowScheduler;
    private final AtomicBoolean isShuttingDown;

    public WorkflowManager(BlockingQueue<WorkflowTrigger> workflowTriggerQueue,
                           BlockingQueue<Message<String, ResultMessage>> workflowResultQueue,
                           Workflow workflow,
                           WorkflowScheduler workflowScheduler) {
        this.workflowTriggerQueue = requireNonNull(workflowTriggerQueue, "workflow trigger queue must not be null");
        this.workflowResultQueue = requireNonNull(workflowResultQueue, "workflow result queue must not be null");
        this.workflow = requireNonNull(workflow, "workflow must not be null");
        this.workflowScheduler = requireNonNull(workflowScheduler, "workflow scheduler must not be null");
        this.isShuttingDown = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        initShutdownAction();

        while (!isShuttingDown.get()) {
            try {
                var trigger = workflowTriggerQueue.take();
                LOGGER.debug("received new input - scheduling workflow run");

                var workflowContext = trigger.workflowContext()
                        .registerScoringSystemResultListener(getResultMessageToResultQueueActor());

                workflowScheduler.enqueueWorkflowRun(workflow, trigger.patientMedicalCaseIdentifier(),
                                workflowContext, trigger.workflowInputParameters())
                        .thenAccept(res -> {
                            if (res.isLeft()) {
                                if (res.getLeft().cause().isPresent()) {
                                    LOGGER.warn("enqueued workflow failed", res.getLeft().cause().get());
                                } else {
                                    LOGGER.warn("enqueued workflow failed");
                                }
                            }
                        })
                        .exceptionally(err -> {
                            LOGGER.warn("enqueued workflow failed unexpectedly", err);
                            return null;
                        });
            } catch (InterruptedException e) {
                LOGGER.warn("got interrupted while waiting for new input - probably due to a shutdown request");
            }
        }
    }

    private void initShutdownAction() {
        var workflowPipelineThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isShuttingDown.set(true);
            workflowPipelineThread.interrupt();
        }));
    }

    private Consumer<Message<String, ResultMessage>> getResultMessageToResultQueueActor() {
        return workflowResultQueue::add;
    }
}
