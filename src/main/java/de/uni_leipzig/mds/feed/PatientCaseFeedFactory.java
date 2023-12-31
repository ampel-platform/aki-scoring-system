package de.uni_leipzig.mds.feed;

import org.bson.types.ObjectId;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

public final class PatientCaseFeedFactory {

    private PatientCaseFeedFactory() {
    }

    public static PatientCaseFeed createNewMinimumPatientCaseFeed(String patientId, String caseId, Date dateOfBirth) {
        var patientCaseFeedId = createObjectId();
        var patient = new Patient(patientId, dateOfBirth, false);
        return new PatientCaseFeed(patientCaseFeedId, patient, caseId);
    }

    private static ObjectId createObjectId() {
        return new ObjectId(Timestamp.from(Instant.now()));
    }
}
