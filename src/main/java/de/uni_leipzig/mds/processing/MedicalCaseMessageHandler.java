package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.medcase.MedCaseMessage;
import de.uni_leipzig.mds.db.IAkiRepo;
import de.uni_leipzig.mds.db.MedicalCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

// TODO: docs
public final class MedicalCaseMessageHandler implements MessageHandler<MedCaseMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MedicalCaseMessageHandler.class);

    private final IAkiRepo akiRepo;

    public MedicalCaseMessageHandler(IAkiRepo akiRepo) {
        this.akiRepo = requireNonNull(akiRepo, "aki repository must not be null");
    }

    @Override
    public void handleMessage(Message<String, MedCaseMessage> message) {
        var medicalCaseMessagePayload = message.body();
        LOGGER.debug("inserting medical case with id `{}`", medicalCaseMessagePayload.caseId());

        akiRepo.upsertMedicalCase(
                new MedicalCase(
                        medicalCaseMessagePayload.caseId(),
                        medicalCaseMessagePayload.patientId(),
                        medicalCaseMessagePayload.creationDate(),
                        medicalCaseMessagePayload.cancellationDate()));
    }
}
