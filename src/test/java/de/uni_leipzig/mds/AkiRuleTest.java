package de.uni_leipzig.mds;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static de.uni_leipzig.mds.io.proto.generated.rule_evaluation_result.AmpelStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class AkiRuleTest {

    private final AkiRule akiRule = new AkiRule();

    @Test
    public void ampel_should_be_green_if_patient_is_not_yet_18() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(10)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(10.0f)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertTrue(evaluationResult.getSeverity().isBlank());
        assertTrue(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_green_if_there_is_no_additional_value_within_the_last_168h() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(10.0f)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertTrue(evaluationResult.getSeverity().isBlank());
        assertTrue(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_green_if_creatinine_has_not_increased_by_50_percent_compared_to_168h_value() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(10.0f)
                .minCreatinineValue48Hours(null)
                .minCreatinineValue168Hours(8.0f)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertTrue(evaluationResult.getSeverity().isBlank());
        assertTrue(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_green_if_creatinine_has_not_increased_by_26_5_compared_to_48h_value_and_not_increased_by_50_percent_compared_to_168h_value() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(10.0f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(8.0f)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertTrue(evaluationResult.getSeverity().isBlank());
        assertTrue(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_green_after_creatinine_increase_of_26_5_compared_to_48h_value_because_of_a_dialysis() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(8.0f)
                .hadDialysisInLast168Hours(true)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("AKIN Dialy", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_green_after_creatinine_increase_of_50_percent_compared_to_168h_value_because_of_dialysis() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(null)
                .minCreatinineValue168Hours(8.0f)
                .hadDialysisInLast168Hours(true)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(GREEN, evaluationResult.getStatus());
        assertEquals("AKIN Dialy", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_red_after_3_fold_increase_of_creatinine_compared_to_168h_value_if_vancomycin_request_present() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(8.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(true)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(RED, evaluationResult.getStatus());
        assertEquals("AKIN3 VANC", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_yellow_after_3_fold_increase_of_creatinine_compared_to_168h_value_if_vancomycin_request_absent() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(8.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(false)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(YELLOW, evaluationResult.getStatus());
        assertEquals("AKIN3", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_red_after_2_fold_increase_of_creatinine_compared_to_168h_value_if_vancomycin_request_present() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(15.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(true)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(RED, evaluationResult.getStatus());
        assertEquals("AKIN2 VANC", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_yellow_after_2_fold_increase_of_creatinine_compared_to_168h_value_if_vancomycin_request_absent() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(15.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(false)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(YELLOW, evaluationResult.getStatus());
        assertEquals("AKIN2", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_red_if_vancomycin_request_present() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(30.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(true)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(RED, evaluationResult.getStatus());
        assertEquals("AKIN1 VANC", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }

    @Test
    public void ampel_should_be_yellow_if_vancomycin_request_absent() {
        var akiTestData = AkiData.builder()
                .patientNumber("123")
                .patientCaseNumber("ABC")
                .patientBirthdate(Timestamp.valueOf(LocalDateTime.now().minusYears(19)))
                .analyteMeasurementTime(Timestamp.valueOf(LocalDateTime.now()))
                .creatinineValue(36.5f)
                .minCreatinineValue48Hours(10.0f)
                .minCreatinineValue168Hours(30.0f)
                .hadDialysisInLast168Hours(false)
                .hadVancomycinInLast168Hours(false)
                .build();

        var evaluationResult = akiRule.evaluate(akiTestData);

        assertEquals(YELLOW, evaluationResult.getStatus());
        assertEquals("AKIN1", evaluationResult.getSeverity());
        assertEquals("123", evaluationResult.getPatientId());
        assertEquals("ABC", evaluationResult.getPatientCaseId());
        assertFalse(evaluationResult.getSeverity().isBlank());
        assertFalse(evaluationResult.getSeverityComment().isBlank());
    }
}
