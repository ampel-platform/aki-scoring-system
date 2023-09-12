package de.uni_leipzig.mds;

import de.uni_leipzig.mds.evaluation.Rule;
import de.uni_leipzig.mds.io.proto.TypeUtil;
import de.uni_leipzig.mds.io.proto.generated.rule_evaluation_result.RuleEvaluationResultProtoBuf;
import de.uni_leipzig.mds.io.proto.generated.rule_evaluation_result.RuleEvaluationResultProtoBuf.Builder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static de.uni_leipzig.mds.io.proto.generated.rule_evaluation_result.AmpelStatus.*;

public class AkiRule implements Rule<AkiData> {

    private enum State {
        AGE_CHECK {
            @Override
            State transition(AkiData akiData) {
                return (ChronoUnit.YEARS.between(akiData.getPatientBirthdate().toLocalDateTime(),
                        LocalDateTime.now()) < 18)
                        ? State.AMPEL_GREEN
                        : State.PREVIOUS_CREATININE_VALUE_168H_PRESENT;
            }
        },
        PREVIOUS_CREATININE_VALUE_168H_PRESENT {
            @Override
            State transition(AkiData akiData) {
                return (akiData.getMinCreatinineValue168Hours() == null)
                        ? State.AMPEL_GREEN
                        : State.PREVIOUS_CREATININE_VALUE_48H_PRESENT;
            }
        },
        PREVIOUS_CREATININE_VALUE_48H_PRESENT {
            @Override
            State transition(AkiData akiData) {
                return (akiData.getMinCreatinineValue48Hours() != null)
                        ? State.CHECK_CREATININE_INCREASE_AGAINST_48H
                        : State.CHECK_CREATININE_INCREASE_AGAINST_168H;
            }
        },
        CHECK_CREATININE_INCREASE_AGAINST_168H {
            @Override
            State transition(AkiData akiData) {
                return ((akiData.getCreatinineValue() / akiData.getMinCreatinineValue168Hours()) >= 1.5)
                        ? State.CHECK_DIALYSIS_168H
                        : State.AMPEL_GREEN;
            }
        },

        CHECK_CREATININE_INCREASE_AGAINST_48H {
            @Override
            State transition(AkiData akiData) {
                return ((akiData.getCreatinineValue() - akiData.getMinCreatinineValue48Hours()) >= 26.5)
                        ? State.CHECK_DIALYSIS_168H
                        : State.CHECK_CREATININE_INCREASE_AGAINST_168H;
            }
        },

        CHECK_DIALYSIS_168H {
            @Override
            State transition(AkiData akiData) {
                return akiData.isHadDialysisInLast168Hours()
                        ? State.AMPEL_GREEN_DIALYSIS
                        : State.CHECK_CREATININE_3_FOLD_INCREASE_168H;
            }
        },

        CHECK_CREATININE_3_FOLD_INCREASE_168H {
            @Override
            State transition(AkiData akiData) {
                return ((akiData.getCreatinineValue() / akiData.getMinCreatinineValue168Hours()) >= 3.0)
                        ? State.CHECK_AKIN3_VANCOMYCIN
                        : State.CHECK_CREATININE_2_FOLD_INCREASE_168H;
            }
        },

        CHECK_CREATININE_2_FOLD_INCREASE_168H {
            @Override
            State transition(AkiData akiData) {
                return ((akiData.getCreatinineValue() / akiData.getMinCreatinineValue168Hours()) >= 2.0)
                        ? State.CHECK_AKIN2_VANCOMYCIN
                        : State.CHECK_AKIN1_VANCOMYCIN;
            }
        },

        CHECK_AKIN3_VANCOMYCIN {
            @Override
            State transition(AkiData akiData) {
                return (akiData.isHadVancomycinInLast168Hours())
                        ? State.AMPEL_RED_AKIN3
                        : State.AMPEL_YELLOW_AKIN3;
            }
        },

        CHECK_AKIN2_VANCOMYCIN {
            @Override
            State transition(AkiData akiData) {
                return (akiData.isHadVancomycinInLast168Hours())
                        ? State.AMPEL_RED_AKIN2
                        : State.AMPEL_YELLOW_AKIN2;
            }
        },

        CHECK_AKIN1_VANCOMYCIN {
            @Override
            State transition(AkiData akiData) {
                return (akiData.isHadVancomycinInLast168Hours())
                        ? State.AMPEL_RED_AKIN1
                        : State.AMPEL_YELLOW_AKIN1;
            }
        },

        AMPEL_GREEN {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(GREEN)
                                .build()
                );
            }
        },

        AMPEL_GREEN_DIALYSIS {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(GREEN)
                                .setSeverity("AKIN Dialy")
                                .setSeverityComment("Nach KDIGO-Kriterien liegt eine akute Nierenschädigung vor. Auf " +
                                        "Grund einer dokumentierten Dialyse innerhalb der letzten 7 Tage wurde der " +
                                        "AKI-Alarm deaktiviert.")
                                .build()
                );
            }
        },

        AMPEL_YELLOW_AKIN3 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(YELLOW)
                                .setSeverity("AKIN3")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung hin " +
                                        "(AKIN-Stadium 3 nach KDIGO-Kriterien). Weiterführende Diagnostik- und " +
                                        "Therapiemaßnahmen (KDIGO-Bundle) undnephrologisches Konsil empfohlen.")
                                .build()
                );
            }
        },

        AMPEL_YELLOW_AKIN2 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(YELLOW)
                                .setSeverity("AKIN2")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung hin " +
                                        "(AKIN-Stadium 2 nach KDIGO-Kriterien). Weiterführende Diagnostik- und " +
                                        "Therapiemaßnahmen (KDIGO-Bundle) und ggf. nephrologisches Konsil empfohlen.")
                                .build()
                );
            }
        },

        AMPEL_YELLOW_AKIN1 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(YELLOW)
                                .setSeverity("AKIN1")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung hin " +
                                        "(AKIN-Stadium 1 nach KDIGO-Kriterien). Weiterführende Diagnostik- und " +
                                        "Therapiemaßnahmen (KDIGO-Bundle) empfohlen.")
                                .build()
                );
            }
        },

        AMPEL_RED_AKIN3 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(RED)
                                .setSeverity("AKIN3 VANC")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung im " +
                                        "Zusammenhang mit einer Vancomycintherapie hin (AKIN-Stadium 3 nach " +
                                        "KDIGO-Kriterien). Weiterführende Diagnostik- und Therapiemaßnahmen " +
                                        "(KDIGO-Bundle) undnephrologisches Konsil empfohlen.")
                                .build()
                );
            }
        },

        AMPEL_RED_AKIN2 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(RED)
                                .setSeverity("AKIN2 VANC")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung im " +
                                        "Zusammenhang mit einer Vancomycintherapie hin (AKIN-Stadium 2 nach " +
                                        "KDIGO-Kriterien). Weiterführende Diagnostik- und Therapiemaßnahmen " +
                                        "(KDIGO-Bundle) und ggf. nephrologisches Konsil empfohlen.")
                                .build()
                );
            }
        },

        AMPEL_RED_AKIN1 {
            @Override
            boolean isFinalState() {
                return true;
            }

            @Override
            Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
                return Optional.of(
                        State.populateBaseRuleEvaluationResultProtoBufBuilder(akiData)
                                .setStatus(RED)
                                .setSeverity("AKIN1 VANC")
                                .setSeverityComment("Der Kreatininanstieg weist auf eine akute Nierenschädigung im " +
                                        "Zusammenhang mit einerVancomycintherpie hin (AKIN-Stadium 1 nach " +
                                        "KDIGO-Kriterien). Weiterführende Diagnostik- und Therapiemaßnahmen " +
                                        "(KDIGO-Bundle) empfohlen.")
                                .build()
                );
            }
        };


        State transition(AkiData akiData) {
            return this;
        }

        boolean isFinalState() {
            return false;
        }

        Optional<RuleEvaluationResultProtoBuf> getFinalStateResult(AkiData akiData) {
            return Optional.empty();
        }

        private static Builder populateBaseRuleEvaluationResultProtoBufBuilder(AkiData akiData) {
            return RuleEvaluationResultProtoBuf.newBuilder()
                    .setRuleName("AKI")
                    .setRuleVersion("v1.0")
                    .setPatientId(akiData.getPatientNumber())
                    .setPatientCaseId(akiData.getPatientCaseNumber())
                    .setAnalyteName("creatinine")
                    .setAnalyteValue(String.valueOf(akiData.getCreatinineValue()))
                    .setAnalyteValueUnit("µmol/L")
                    .setAnalyteMeasurementDateTime(
                            TypeUtil.toProtoBufTimestamp(akiData.getAnalyteMeasurementTime())
                    )
                    .setResultCreationDateTime(
                            TypeUtil.toProtoBufTimestamp(Timestamp.valueOf(LocalDateTime.now()))
                    );
        }
    }


    @Override
    public RuleEvaluationResultProtoBuf evaluate(AkiData akiData) {
        var ruleState = State.AGE_CHECK;

        do {
            ruleState = ruleState.transition(akiData);
        } while (!ruleState.isFinalState());

        var finalStateResult = ruleState.getFinalStateResult(akiData);
        if (finalStateResult.isPresent()) {
            return finalStateResult.get();
        } else {
            throw new IllegalStateException("rule evaluation transitioned to an illegal state");
        }
    }
}
