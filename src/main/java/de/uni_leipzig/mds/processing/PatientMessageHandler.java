package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.patient.PatientMessage;
import de.uni_leipzig.mds.db.IAkiRepo;
import de.uni_leipzig.mds.db.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

// TODO: docs
public final class PatientMessageHandler implements MessageHandler<PatientMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientMessageHandler.class);

    private final IAkiRepo akiRepo;

    public PatientMessageHandler(IAkiRepo akiRepo) {
        this.akiRepo = requireNonNull(akiRepo, "aki repository must not be null");
    }

    @Override
    public void handleMessage(Message<String, PatientMessage> message) {
        var patientMessagePayload = message.body();

        LOGGER.debug("inserting patient with id `{}`", patientMessagePayload.patientId());
        akiRepo.upsertPatient(
                new Patient(
                        patientMessagePayload.patientId(),
                        patientMessagePayload.dateOfBirth(),
                        patientMessagePayload.deceased(),
                        patientMessagePayload.dischargeTimestamp()));
    }
}
