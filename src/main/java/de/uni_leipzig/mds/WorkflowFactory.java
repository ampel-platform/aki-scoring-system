package de.uni_leipzig.mds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import de.uni_leipzig.mds.common.types.proto.TypeUtil;
import de.uni_leipzig.mds.common.types.proto.generated.ampel_event.AmpelEventProtoBuf;
import de.uni_leipzig.mds.common.types.proto.generated.ampel_event.AmpelStatus;
import de.uni_leipzig.mds.data.db.LaboratoryValue;
import de.uni_leipzig.mds.data.db.PatientCaseFeed;
import de.uni_leipzig.mds.evaluation.workflow.Step;
import de.uni_leipzig.mds.evaluation.workflow.Transition;
import de.uni_leipzig.mds.evaluation.workflow.Workflow;
import de.uni_leipzig.mds.evaluation.workflow.task.Runner;
import de.uni_leipzig.mds.io.connection.AmpelMessage;
import de.uni_leipzig.mds.io.connection.serialization.types.PatientCaseKey;
import io.vavr.control.Either;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;

/**
 * Factory for creating a {@link Workflow} that can run an evaluation for acute kidney injury (AKI).
 */
public class WorkflowFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowFactory.class);

    public static final String PATIENT_FEED_PARAM = "PatientFeed";
    private static final String TRIGGER_ANALYTE = "TriggerAnalyte";
    private static final String LOWEST_CRE_48H_PARAM = "CreMin48";
    private static final String LOWEST_CRE_168H_PARAM = "CreMin168";

    private WorkflowFactory() {
    }

    /**
     * Creates a {@link Workflow} that evaluates a {@link PatientCaseFeed} with respect to an acute kidney injury (AKI).
     *
     * @return The {@link Workflow} instance.
     */
    public static Workflow createAkiWorkflow() {
        var summaryBuilder = Summary.builder();

        return Workflow.workflow("aki-ruleset",
                        Step.step("preparation",
                                Runner.runner().apply((ctx, inputParams) -> {
                                    var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);

                                    // This has to be there otherwise the workflow wouldn't have been started.
                                    var triggerValue = patientFeed.getCreatinineValues().getFirst();

                                    summaryBuilder.patientDateOfBirth(patientFeed.getDateOfBirth());
                                    return Either.right(Optional.of(
                                            inputParams.set(TRIGGER_ANALYTE, triggerValue)
                                    ));
                                })))
                .withStep(Step.step("is-of-full-age",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var ageYears = Math.abs(ChronoUnit.YEARS.between(triggerValue.getTime(),
                                    patientFeed.getDateOfBirth()));

                            return Either.right(Optional.of(
                                    inputParams.set("result", (ageYears >= 18))
                            ));
                        })))
                .withStep(Step.step("check-previous-cre-value-168h-exists",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var previousCreValueExists = !patientFeed.getCreatinineValues().stream()
                                    .skip(1)
                                    .takeWhile(labValue -> labValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(168))
                                            && labValue.getTime().isBefore(triggerValue.getTime()))
                                    .toList()
                                    .isEmpty();

                            return Either.right(Optional.of(
                                    inputParams.set("result", previousCreValueExists)
                            ));
                        })))
                .withStep(Step.step("calculate-cre-min-values",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var minCre168 = patientFeed.getCreatinineValues().stream()
                                    .skip(1)
                                    .filter(labValue -> labValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(168))
                                            && labValue.getTime().isBefore(triggerValue.getTime()))
                                    .min(Comparator.comparing(LaboratoryValue::getValue));

                            var minCre48 = patientFeed.getCreatinineValues().stream()
                                    .skip(1)
                                    .filter(labValue -> labValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(48))
                                            && labValue.getTime().isBefore(triggerValue.getTime()))
                                    .min(Comparator.comparing(LaboratoryValue::getValue));

                            summaryBuilder.minCre48(minCre48.map(LaboratoryValue::getValue).orElse(null));
                            summaryBuilder.minCre168(minCre168.map(LaboratoryValue::getValue).orElse(null));

                            return Either.right(Optional.of(
                                    inputParams
                                            .set(LOWEST_CRE_48H_PARAM, minCre48)
                                            .set(LOWEST_CRE_168H_PARAM, minCre168)
                            ));
                        })))
                .withStep(Step.step("check-cre-min-48h-exists",
                        Runner.runner().apply((ctx, inputParams) -> {
                            //noinspection unchecked
                            var minCre48 = (Optional<LaboratoryValue>) inputParams.get(LOWEST_CRE_48H_PARAM);

                            return Either.right(Optional.of(
                                    inputParams.set("result", minCre48.isPresent())
                            ));
                        })))
                .withStep(Step.step("check-cre-increase-against-48h",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);
                            //noinspection unchecked
                            var minCre48 = (Optional<LaboratoryValue>) inputParams.get(LOWEST_CRE_48H_PARAM);

                            //noinspection OptionalGetWithoutIsPresent
                            var result = (triggerValue.getValue() - minCre48.get().getValue()) >= 26.5;

                            return Either.right(Optional.of(
                                    inputParams.set("result", result)
                            ));
                        })))
                .withStep(Step.step("check-cre-increase-against-168h",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);
                            //noinspection unchecked
                            var minCre168 = (Optional<LaboratoryValue>) inputParams.get(LOWEST_CRE_168H_PARAM);

                            //noinspection OptionalGetWithoutIsPresent
                            var result = (triggerValue.getValue() / minCre168.get().getValue()) >= 1.5;

                            return Either.right(Optional.of(
                                    inputParams.set("result", result)
                            ));
                        })))
                .withStep(Step.step("check-dialysis-in-last-168h",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var numDialysis168 = patientFeed.getDialysisProcedures().stream()
                                    .filter(procedure -> (
                                            procedure.getTime().isAfter(triggerValue.getTime().minusHours(168))
                                                    && procedure.getTime().isBefore(triggerValue.getTime())
                                    ))
                                    .count();

                            summaryBuilder.dialysis168(numDialysis168 > 0);

                            return Either.right(Optional.of(
                                    inputParams.set("result", numDialysis168 > 0)
                            ));
                        })))
                .withStep(Step.step("check-cre-3-fold-increase-168h",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);
                            //noinspection unchecked
                            var minCre168 = (Optional<LaboratoryValue>) inputParams.get(LOWEST_CRE_168H_PARAM);

                            //noinspection OptionalGetWithoutIsPresent
                            var result = ((triggerValue.getValue() / minCre168.get().getValue()) >= 3.0)
                                    || (triggerValue.getValue() >= 353.6);

                            return Either.right(Optional.of(
                                    inputParams.set("result", result)
                            ));
                        })))
                .withStep(Step.step("check-cre-2-fold-increase-168h",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);
                            //noinspection unchecked
                            var minCre168 = (Optional<LaboratoryValue>) inputParams.get(LOWEST_CRE_168H_PARAM);

                            //noinspection OptionalGetWithoutIsPresent
                            var result = (triggerValue.getValue() / minCre168.get().getValue()) >= 2.0;

                            return Either.right(Optional.of(
                                    inputParams.set("result", result)
                            ));
                        })))
                .withStep(Step.step("check-akin-3-vancomycin",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var numVancomycinInLast168h = patientFeed.getVancomycinValues().stream()
                                    .filter(vancoValue -> vancoValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(168))
                                            && vancoValue.getTime().isBefore(triggerValue.getTime()))
                                    .count();

                            summaryBuilder.vancomycin168(numVancomycinInLast168h > 0);

                            return Either.right(Optional.of(
                                    inputParams.set("result", numVancomycinInLast168h > 0)
                            ));
                        })))
                .withStep(Step.step("check-akin-2-vancomycin",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var numVancomycinInLast168h = patientFeed.getVancomycinValues().stream()
                                    .filter(vancoValue -> vancoValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(168))
                                            && vancoValue.getTime().isBefore(triggerValue.getTime()))
                                    .count();

                            summaryBuilder.vancomycin168(numVancomycinInLast168h > 0);

                            return Either.right(Optional.of(
                                    inputParams.set("result", numVancomycinInLast168h > 0)
                            ));
                        })))
                .withStep(Step.step("check-akin-1-vancomycin",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            var numVancomycinInLast168h = patientFeed.getVancomycinValues().stream()
                                    .filter(vancoValue -> vancoValue.getTime()
                                            .isAfter(triggerValue.getTime().minusHours(168))
                                            && vancoValue.getTime().isBefore(triggerValue.getTime()))
                                    .count();

                            summaryBuilder.vancomycin168(numVancomycinInLast168h > 0);

                            return Either.right(Optional.of(
                                    inputParams.set("result", numVancomycinInLast168h > 0)
                            ));
                        })))
                .withStep(Step.step("ampel-green",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.GREEN)
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-green-akin-dialy",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.GREEN)
                                                        .setSeverity("AKIN Dialy")
                                                        .setSeverityComment("Nach KDIGO-Kriterien liegt eine akute " +
                                                                "Nierenschädigung vor. Auf Grund einer dokumentierten Dialyse " +
                                                                "innerhalb der letzten 7 Tage wurde der AKI-Alarm deaktiviert.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-yellow-akin-1",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.YELLOW)
                                                        .setSeverity("AKIN1")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung hin (AKIN-Stadium 1 nach KDIGO-Kriterien). " +
                                                                "Weiterführende Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) " +
                                                                "empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-yellow-akin-2",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.YELLOW)
                                                        .setSeverity("AKIN2")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung hin (AKIN-Stadium 2 nach KDIGO-Kriterien). " +
                                                                "Weiterführende Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) " +
                                                                "und ggf. nephrologisches Konsil empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-yellow-akin-3",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.YELLOW)
                                                        .setSeverity("AKIN3")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung hin (AKIN-Stadium 3 nach KDIGO-Kriterien). " +
                                                                "Weiterführende Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) " +
                                                                "und nephrologisches Konsil empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-red-akin-1",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.RED)
                                                        .setSeverity("AKIN1")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung im Zusammenhang mit einer Vancomycintherpie hin" +
                                                                " (AKIN-Stadium 1 nach KDIGO-Kriterien). Weiterführende " +
                                                                "Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-red-akin-2",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.RED)
                                                        .setSeverity("AKIN2")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung im Zusammenhang mit einer Vancomycintherapie " +
                                                                "hin (AKIN-Stadium 2 nach KDIGO-Kriterien). Weiterführende " +
                                                                "Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) und ggf. " +
                                                                "nephrologisches Konsil empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withStep(Step.step("ampel-red-akin-3",
                        Runner.runner().apply((ctx, inputParams) -> {
                            var patientFeed = (PatientCaseFeed) inputParams.get(PATIENT_FEED_PARAM);
                            var triggerValue = (LaboratoryValue) inputParams.get(TRIGGER_ANALYTE);

                            try {
                                ctx.emitAmpelEvent(
                                        new AmpelMessage<>(
                                                new PatientCaseKey(patientFeed.getPatientId(),
                                                        patientFeed.getCaseId()),
                                                AmpelEventProtoBuf.newBuilder()
                                                        .setRuleName("AKI")
                                                        .setRuleVersion("v0.1.0")
                                                        .setPatientId(patientFeed.getPatientId())
                                                        .setPatientCaseId(patientFeed.getCaseId())
                                                        .setStatus(AmpelStatus.RED)
                                                        .setSeverity("AKIN3")
                                                        .setSeverityComment("Der Kreatininanstieg weist auf eine akute " +
                                                                "Nierenschädigung im Zusammenhang mit einer Vancomycintherapie " +
                                                                "hin (AKIN-Stadium 3 nach KDIGO-Kriterien). Weiterführende " +
                                                                "Diagnostik- und Therapiemaßnahmen (KDIGO-Bundle) und " +
                                                                "nephrologisches Konsil empfohlen.")
                                                        .setAnalyteName(triggerValue.getAnalyteCode())
                                                        .setAnalyteValue(triggerValue.getValue().toString())
                                                        .setAnalyteValueUnit(triggerValue.getUnit())
                                                        .setAnalyteMeasurementDateTime(TypeUtil.toProtoBufTimestamp(
                                                                triggerValue.getTime(), ZoneOffset.UTC
                                                        ))
                                                        .setResultCreationDateTime(TypeUtil.toProtoBufTimestamp(
                                                                LocalDateTime.now(ZoneOffset.UTC), ZoneOffset.UTC
                                                        ))
                                                        .setMeta(createSummaryInfoStruct(summaryBuilder.build()))
                                                        .build()
                                        )
                                );
                            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                                LOGGER.error("could not build summary information", e);
                            }
                            return Either.right(Optional.empty());
                        })))
                .withTransition(Transition.transition()
                        .from("preparation")
                        .to("is-of-full-age"))
                .withTransition(Transition.transition()
                        .from("is-of-full-age")
                        .to("ampel-green")
                        .when((parameter -> !((Boolean) parameter.get("result")))))
                .withTransition(Transition.transition()
                        .from("is-of-full-age")
                        .to("check-previous-cre-value-168h-exists")
                        .when((parameter -> (Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-previous-cre-value-168h-exists")
                        .to("ampel-green")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-previous-cre-value-168h-exists")
                        .to("calculate-cre-min-values")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("calculate-cre-min-values")
                        .to("check-cre-min-48h-exists"))
                .withTransition(Transition.transition()
                        .from("check-cre-min-48h-exists")
                        .to("check-cre-increase-against-48h")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-cre-min-48h-exists")
                        .to("check-cre-increase-against-168h")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-increase-against-48h")
                        .to("check-cre-increase-against-168h")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-increase-against-168h")
                        .to("ampel-green")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-increase-against-48h")
                        .to("check-dialysis-in-last-168h")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-cre-increase-against-168h")
                        .to("check-dialysis-in-last-168h")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-dialysis-in-last-168h")
                        .to("ampel-green-akin-dialy")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-dialysis-in-last-168h")
                        .to("check-cre-3-fold-increase-168h")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-3-fold-increase-168h")
                        .to("check-akin-3-vancomycin")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-akin-3-vancomycin")
                        .to("ampel-red-akin-3")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-akin-3-vancomycin")
                        .to("ampel-yellow-akin-3")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-3-fold-increase-168h")
                        .to("check-cre-2-fold-increase-168h")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-2-fold-increase-168h")
                        .to("check-akin-2-vancomycin")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-akin-2-vancomycin")
                        .to("ampel-red-akin-2")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-akin-2-vancomycin")
                        .to("ampel-yellow-akin-2")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-cre-2-fold-increase-168h")
                        .to("check-akin-1-vancomycin")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .withTransition(Transition.transition()
                        .from("check-akin-1-vancomycin")
                        .to("ampel-red-akin-1")
                        .when(parameter -> (Boolean) parameter.get("result")))
                .withTransition(Transition.transition()
                        .from("check-akin-1-vancomycin")
                        .to("ampel-yellow-akin-1")
                        .when(parameter -> !((Boolean) parameter.get("result"))))
                .build();
    }

    private static Struct createSummaryInfoStruct(Summary summary) throws JsonProcessingException,
            InvalidProtocolBufferException {
        var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        var summaryJson = mapper.writeValueAsString(summary);
        var structBuilder = Struct.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(summaryJson, structBuilder);
        return structBuilder.build();
    }

    @Data
    @Builder
    private static class Summary {
        @JsonProperty("patient_date_of_birth")
        private final LocalDateTime patientDateOfBirth;
        //        @JsonProperty("trigger_analyte_value")
//        private final double triggerAnalyteValue;
        @JsonProperty("min_cre_48")
        private Double minCre48;
        @JsonProperty("min_cre_168")
        private Double minCre168;
        @JsonProperty("vancomycin_168")
        private Boolean vancomycin168;
        @JsonProperty("dialysis_168")
        private Boolean dialysis168;
    }
}
