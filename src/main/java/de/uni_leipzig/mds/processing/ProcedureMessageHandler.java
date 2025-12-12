package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.mds.db.MessagePersistenceHandler;
import de.uni_leipzig.mds.db.types.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

// TODO: docs
public final class ProcedureMessageHandler implements MessageHandler<ProcedureMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcedureMessageHandler.class);

    /**
     * Codes that denote a dialysis procedure.
     */
    private static final List<String> PROCEDURE_CODES_DIALYSIS = List.of("8-853", "8-854", "8-855", "8-857", "8-85A");

    private final MessagePersistenceHandler messagePersistenceHandler;

    public ProcedureMessageHandler(MessagePersistenceHandler messagePersistenceHandler) {
        this.messagePersistenceHandler = requireNonNull(messagePersistenceHandler, "message persistence handler must not be null");
    }

    @Override
    public void handleMessage(Message<String, ProcedureMessage> message) {
        LOGGER.debug("received procedure message");
        var procedureMessagePayload = message.body();

        if (!isConsiderableProcedure().test(procedureMessagePayload)) {
            LOGGER.warn("procedure message `{}` won't get handled since it was filtered out",
                    procedureMessagePayload.procedureId());
            return;
        }

        LOGGER.debug("inserting procedure with id `{}`", procedureMessagePayload.procedureId());
        messagePersistenceHandler.writeProcedure(
                new Procedure(
                        procedureMessagePayload.caseId(),
                        procedureMessagePayload.procedureId(),
                        procedureMessagePayload.procedureCode(),
                        procedureMessagePayload.performedOn(),
                        procedureMessagePayload.cancellationDate().orElse(null))
        );
    }

    private Predicate<ProcedureMessage> isConsiderableProcedure() {
        return (message) -> {
            var isDialysis = PROCEDURE_CODES_DIALYSIS.stream()
                    .anyMatch(code -> message.procedureCode().toUpperCase().contains(code.toUpperCase()));

            if (!isDialysis) {
                LOGGER.warn("procedure message `{}` is not a dialysis procedure", message.procedureId());
            }
            return isDialysis;
        };
    }
}
