package de.uni_leipzig.mds.feed.repo;

import com.mongodb.client.MongoCollection;
import de.uni_leipzig.mds.feed.DTOUtil;
import de.uni_leipzig.mds.feed.PatientCaseFeedDTO;
import de.uni_leipzig.mds.feed.PatientCaseFeedFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class PatientCaseFeedMongoDBRepositoryTest {


    @Mock
    private MongoCollection<PatientCaseFeedDTO> collection;

    @InjectMocks
    private PatientCaseFeedMongoDBRepository repo;

    @Nested
    class InsertionTests {

        @Test
        public void nullInsertionIsProhibited() {
            assertThrows(NullPointerException.class, () -> repo.insertFeed(null));
            verifyNoInteractions(collection);
        }
    }

    @Nested
    class LookupTests {

        private static Stream<Arguments> provideValuesForNullValuesAreProhibited() {
            return Stream.of(
                    Arguments.of(null, "123"),
                    Arguments.of("A", null),
                    Arguments.of(null, null)
            );
        }

        private static Stream<Arguments> provideValuesForBlankValuesAreProhibited() {
            return Stream.of(
                    Arguments.of("", "123"),
                    Arguments.of("A", ""),
                    Arguments.of("", ""),
                    Arguments.of("\t", "123"),
                    Arguments.of("\n", "123"),
                    Arguments.of("A", "\t"),
                    Arguments.of("A", "\n"),
                    Arguments.of("\t", "\t"),
                    Arguments.of("\t", "\n"),
                    Arguments.of("\n", "\t"),
                    Arguments.of("\n", "\n")
            );
        }

        @ParameterizedTest
        @MethodSource("provideValuesForNullValuesAreProhibited")
        public void nullValuesAreProhibited(String patientId, String caseId) {
            assertThrows(NullPointerException.class, () -> repo.findFeed(patientId, caseId));
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("provideValuesForBlankValuesAreProhibited")
        public void blankValuesAreProhibited(String patientId, String caseId) {
            assertThrows(IllegalArgumentException.class, () -> repo.findFeed(patientId, caseId));
            verifyNoInteractions(collection);
        }
    }

    @Nested
    class ReplacementTests {

        private static Stream<Arguments> provideValuesForNullValuesAreProhibited() {
            var dummyPCF = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());
            var dummyPCFDto = DTOUtil.dtoFromDomain(dummyPCF);

            return Stream.of(
                    Arguments.of(null, "123", 1, dummyPCFDto),
                    Arguments.of("A", null, 1, dummyPCFDto),
                    Arguments.of("A", "123", 1, null),
                    Arguments.of(null, "123", 1, null),
                    Arguments.of(null, null, 1, null)
            );
        }

        private static Stream<Arguments> provideValuesForBlankValuesAreProhibited() {
            var dummyPCF = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());
            var dummyPCFDto = DTOUtil.dtoFromDomain(dummyPCF);

            return Stream.of(
                    Arguments.of("", "123", 1, dummyPCFDto),
                    Arguments.of("A", "", 1, dummyPCFDto),
                    Arguments.of("", "", 1, dummyPCFDto),
                    Arguments.of("\t", "123", 1, dummyPCFDto),
                    Arguments.of("\n", "123", 1, dummyPCFDto),
                    Arguments.of("A", "\t", 1, dummyPCFDto),
                    Arguments.of("A", "\n", 1, dummyPCFDto),
                    Arguments.of("\t", "\t", 1, dummyPCFDto),
                    Arguments.of("\t", "\n", 1, dummyPCFDto),
                    Arguments.of("\n", "\t", 1, dummyPCFDto),
                    Arguments.of("\n", "\n", 1, dummyPCFDto)
            );
        }

        private static Stream<Arguments> provideValuesForNonIncreasingVersionsAreProhibited() {
            var dummyPCF = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());
            var dummyPCFDto = DTOUtil.dtoFromDomain(dummyPCF);

            return Stream.of(
                    Arguments.of("A", "123", 1, dummyPCFDto),
                    Arguments.of("A", "123", 2, dummyPCFDto),
                    Arguments.of("A", "123", Integer.MAX_VALUE, dummyPCFDto)
            );
        }

        @ParameterizedTest
        @MethodSource("provideValuesForNullValuesAreProhibited")
        public void nullValuesAreProhibited(String patientId, String caseId, int expectedCurrentVersion,
                                            PatientCaseFeedDTO updatedInstance) {
            assertThrows(NullPointerException.class, () -> repo.replaceFeed(patientId, caseId, expectedCurrentVersion,
                    updatedInstance));
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("provideValuesForBlankValuesAreProhibited")
        public void blankValuesAreProhibited(String patientId, String caseId, int expectedCurrentVersion,
                                             PatientCaseFeedDTO updatedInstance) {
            assertThrows(IllegalArgumentException.class, () -> repo.replaceFeed(patientId, caseId,
                    expectedCurrentVersion, updatedInstance));
            verifyNoInteractions(collection);
        }

        @ParameterizedTest
        @MethodSource("provideValuesForNonIncreasingVersionsAreProhibited")
        public void nonIncreasingVersionsAreProhibited(String patientId, String caseId, int expectedCurrentVersion,
                                                       PatientCaseFeedDTO updatedInstance) {
            assertThrows(IllegalArgumentException.class, () -> repo.replaceFeed(patientId, caseId,
                    expectedCurrentVersion, updatedInstance));
            verifyNoInteractions(collection);
        }
    }
}
