package de.uni_leipzig.mds.processing;

import de.uni_leipzig.ampel.sdk.io.message.Message;
import de.uni_leipzig.ampel.sdk.io.message.procedure.ProcedureMessage;
import de.uni_leipzig.mds.db.IAkiRepo;
import de.uni_leipzig.mds.db.Procedure;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.requireNonNull;

// TODO: docs
public final class ProcedureMessageHandler implements MessageHandler<ProcedureMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcedureMessageHandler.class);

    /**
     * Denotes the maximum allowed age of a procedure message in days.
     */
    private static final int MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS = 7;

    /**
     * Codes that denote a dialysis procedure.
     */
    private static final List<String> PROCEDURE_CODES_DIALYSIS = List.of("8-853", "8-854", "8-855", "8-857", "8-85A");

    private final IAkiRepo akiRepo;

    public ProcedureMessageHandler(IAkiRepo akiRepo) {
        this.akiRepo = requireNonNull(akiRepo, "aki repository must not be null");
    }

    @Override
    public void handleMessage(Message<String, ProcedureMessage> message) {
        var procedureMessagePayload = message.body();

        var messageFilterCriterion = isConsiderableProcedure()
                .and(Predicate.not(isExceedingMaxAllowedAge()));

        if (!messageFilterCriterion.test(procedureMessagePayload)) {
            LOGGER.warn("procedure message `{}` won't get handled since it was filtered out",
                    procedureMessagePayload.procedureId());
            return;
        }

        LOGGER.debug("inserting procedure with id `{}`", procedureMessagePayload.procedureId());
        akiRepo.insertProcedure(
                new Procedure(
                        ObjectId.get(),
                        procedureMessagePayload.caseId(),
                        procedureMessagePayload.procedureId(),
                        procedureMessagePayload.procedureCode(),
                        procedureMessagePayload.performedOn(),
                        procedureMessagePayload.cancellationDate()));
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

    private Predicate<ProcedureMessage> isExceedingMaxAllowedAge() {
        return (message) -> {
            var ageInDays = Math.abs(message.performedOn().until(LocalDateTime.now(), DAYS));
            var exceedsMaximumAge = ageInDays > MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS;

            if (exceedsMaximumAge) {
                LOGGER.warn("procedure message `{}` exceeds maximum allowed age of `{}` days", message.procedureId(),
                        MAX_PROCEDURE_MESSAGE_AGE_IN_DAYS);
            }
            return exceedsMaximumAge;
        };
    }
}
