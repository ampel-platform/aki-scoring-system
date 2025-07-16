package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.laboratory.LaboratoryMessage;
import de.uni_leipzig.mds.db.MessagePersistenceHandler;
import de.uni_leipzig.mds.db.types.LaboratoryEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class LaboratoryMessageHandler implements MessageHandler<LaboratoryMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaboratoryMessageHandler.class);

    /**
     * Codes that denote a creatinine value.
     */
    static final List<String> LAB_CODES_CREATININE = List.of("LCRE_S");

    /**
     * Codes that denote a vancomycin value.
     */
    static final List<String> LAB_CODES_VANCOMYCIN = List.of("VANC-K_S", "VANC-S_S", "VANC-T_S", "VANC-U_S");

    private final MessagePersistenceHandler messagePersistenceHandler;


    public LaboratoryMessageHandler(MessagePersistenceHandler messagePersistenceHandler) {
        this.messagePersistenceHandler = requireNonNull(messagePersistenceHandler, "message persistence handler must not be null");
    }

    @Override
    public void handleMessage(Message<String, LaboratoryMessage> message) {
        LOGGER.debug("received laboratory message");
        var labMessagePayload = message.body();

        if (!isConsiderableLaboratoryValue().test(labMessagePayload)) {
            LOGGER.warn("laboratory message `{}` won't get handled since it was filtered out",
                    labMessagePayload.documentId());
            return;
        }

        LOGGER.debug("inserting laboratory value with id `{}`", labMessagePayload.documentId());
        messagePersistenceHandler.writeLaboratoryEvaluation(
                new LaboratoryEvaluation(
                        labMessagePayload.caseId(),
                        labMessagePayload.orderId(),
                        labMessagePayload.analyte(),
                        labMessagePayload.analyteValue(),
                        labMessagePayload.analyteUnit(),
                        labMessagePayload.originatingSystem(),
                        labMessagePayload.analyteMeasuringTimestamp()));
    }

    private Predicate<LaboratoryMessage> isConsiderableLaboratoryValue() {
        var considerableLabValueFilter = isResemblingCreatinineValue()
                .or(isResemblingVancomycinValue());

        return (message) -> {
            var isConsiderableLabValue = considerableLabValueFilter.test(message);
            if (!isConsiderableLabValue) {
                LOGGER.warn("laboratory message `{}` does not resemble a creatinine or vancomycin value",
                        message.documentId());
            }
            return isConsiderableLabValue;
        };
    }

    private Predicate<LaboratoryMessage> isResemblingCreatinineValue() {
        return (message) -> LAB_CODES_CREATININE.contains(message.analyte().toUpperCase());
    }

    private Predicate<LaboratoryMessage> isResemblingVancomycinValue() {
        return (message) -> LAB_CODES_VANCOMYCIN.contains(message.analyte().toUpperCase());
    }
}
