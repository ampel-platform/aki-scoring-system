package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.mds.db.MessagePersistenceHandler;
import de.uni_leipzig.mds.db.types.MedicalCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

// TODO: docs
public final class MedicalCaseMessageHandler implements MessageHandler<MedCaseMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalCaseMessageHandler.class);

    private final MessagePersistenceHandler messagePersistenceHandler;

    public MedicalCaseMessageHandler(MessagePersistenceHandler messagePersistenceHandler) {
        this.messagePersistenceHandler = requireNonNull(messagePersistenceHandler, "message persistence handler must not be null");
    }

    @Override
    public void handleMessage(Message<String, MedCaseMessage> message) {
        LOGGER.debug("received medical case message");
        var medicalCaseMessagePayload = message.body();

        LOGGER.debug("inserting medical case with id `{}`", medicalCaseMessagePayload.caseId());
        messagePersistenceHandler.writeMedicalCase(
                new MedicalCase(
                        medicalCaseMessagePayload.caseId(),
                        medicalCaseMessagePayload.patientId(),
                        medicalCaseMessagePayload.creationDate(),
                        medicalCaseMessagePayload.cancellationDate().orElse(null)));
    }
}
