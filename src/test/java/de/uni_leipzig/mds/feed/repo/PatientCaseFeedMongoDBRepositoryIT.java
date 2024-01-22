package de.uni_leipzig.mds.feed.repo;

import de.uni_leipzig.mds.feed.DTOUtil;
import de.uni_leipzig.mds.feed.PatientCaseFeedFactory;
import de.uni_leipzig.mds.feed.repo.PatientCaseFeedRepositoryFactory.MongoDBConnectionSettings;
import de.uni_leipzig.mds.feed.repo.error.InsertionError;
import de.uni_leipzig.mds.feed.repo.error.LookupError;
import de.uni_leipzig.mds.feed.repo.error.ReplacementError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class PatientCaseFeedMongoDBRepositoryIT {

    private PatientCaseFeedRepository repo;

    @Container
    public MongoDBContainer container = new MongoDBContainer(DockerImageName.parse("mongo:7.0.4-jammy"))
            .withExposedPorts(27017)
            .waitingFor(new HttpWaitStrategy()
                    .forPort(27017)
                    .forStatusCode(200));

    @BeforeEach
    public void setUp() {
        repo = PatientCaseFeedRepositoryFactory.newMongoDBRepository(
                new MongoDBConnectionSettings(
                        container.getHost(),
                        container.getFirstMappedPort().toString(),
                        "test-db"
                ),
                "test-app",
                "test"
        );
    }

    @Nested
    class InsertionTests {
        @Test
        public void duplicatePatientCaseIdsAreProhibited() {
            var patientCaseFeed = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());

            var dto = DTOUtil.dtoFromDomain(patientCaseFeed);
            var resultA = repo.insertFeed(dto);
            var resultB = repo.insertFeed(dto);

            assertTrue(resultA.isRight());
            assertTrue(resultB.isLeft());
            assertInstanceOf(InsertionError.PatientCaseAlreadyExists.class, resultB.getLeft());
        }
    }

    @Nested
    class LookupTests {

        @Test
        public void nonExistingFeedCannotBeFound() {
            var result = repo.findFeed("A", "123");

            assertTrue(result.isLeft());
            assertInstanceOf(LookupError.PatientCaseNotFoundError.class, result.getLeft());
        }

        @Test
        public void existingFeedCanBeFound() {
            var patientCaseFeed = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                            LocalDateTime.now())
                    .addCreatinineValue(LocalDateTime.now(), 25.0, "mmol")
                    .addVancomycinValue(LocalDateTime.now(), 1.0, "mmol")
                    .addDialysisProcedure(LocalDateTime.now(), "dialysis-oicd-code");
            repo.insertFeed(DTOUtil.dtoFromDomain(patientCaseFeed));

            var resultDto = repo.findFeed("A", "123");

            assertTrue(resultDto.isRight());
            assertEquals(patientCaseFeed, DTOUtil.domainFromDto(resultDto.get()));
        }
    }

    @Nested
    class ReplacementTests {

        @Test
        public void nonExistingFeedCannotBeReplaced() {
            var patientCaseFeed = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                            LocalDateTime.now())
                    .addCreatinineValue(LocalDateTime.now(), 25.0, "mmol");

            var result = repo.replaceFeed("A", "123", 1, DTOUtil.dtoFromDomain(patientCaseFeed));

            assertTrue(result.isLeft());
            assertInstanceOf(ReplacementError.PatientCaseNotFoundError.class, result.getLeft());
        }

        @Test
        public void existingFeedWithoutMatchingVersionCannotBeReplaced() {
            var patientCaseFeed = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());
            repo.insertFeed(DTOUtil.dtoFromDomain(patientCaseFeed));

            var updatedPatientCaseFeed = patientCaseFeed
                    .addCreatinineValue(LocalDateTime.now(), 25.0, "mmol")
                    .addCreatinineValue(LocalDateTime.now(), 30.0, "mmol");
            var result = repo.replaceFeed("A", "123", 2, DTOUtil.dtoFromDomain(updatedPatientCaseFeed));

            assertTrue(result.isLeft());
            assertInstanceOf(ReplacementError.PatientCaseVersionMismatchError.class, result.getLeft());
        }

        @Test
        public void existingFeedWithMatchingVersionCanBeReplaced() {
            var patientCaseFeed = PatientCaseFeedFactory.createNewMinimumPatientCaseFeed("A", "123",
                    LocalDateTime.now());
            repo.insertFeed(DTOUtil.dtoFromDomain(patientCaseFeed));

            var updatedPatientCaseFeed = patientCaseFeed.addCreatinineValue(LocalDateTime.now(), 25.0, "mmol");
            var result = repo.replaceFeed("A", "123", 1, DTOUtil.dtoFromDomain(updatedPatientCaseFeed));

            assertTrue(result.isRight());
            assertEquals(updatedPatientCaseFeed, DTOUtil.domainFromDto(result.get()));
        }
    }
}
