package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionValidationException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionMessage;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ObjectStorePort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.QueuePublisherPort;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateConversionJobServiceTest {
    @Test
    void rejectsEmptyCode() {
        final var service = new CreateConversionJobService(new InMemoryJobRepository(), new InMemoryObjectStore(), new InMemoryQueuePublisher(), new FixedConfig());
        final CreateConversionRequestModel request = new CreateConversionRequestModel("cobol", "java", "21", "hexagonal", " ", Map.of());

        assertThrows(ConversionValidationException.class, () -> service.execute(request));
    }

    @Test
    void rejectsInvalidLanguage() {
        final var service = new CreateConversionJobService(new InMemoryJobRepository(), new InMemoryObjectStore(), new InMemoryQueuePublisher(), new FixedConfig());
        final CreateConversionRequestModel request = new CreateConversionRequestModel("invalid", "java", "21", "hexagonal", "DISPLAY 'ok'", Map.of());

        assertThrows(ConversionValidationException.class, () -> service.execute(request));
    }

    @Test
    void rejectsEmptyVersionAndType() {
        final var service = new CreateConversionJobService(new InMemoryJobRepository(), new InMemoryObjectStore(), new InMemoryQueuePublisher(), new FixedConfig());
        final CreateConversionRequestModel request = new CreateConversionRequestModel("cobol", "java", " ", " ", "DISPLAY 'ok'", Map.of());

        assertThrows(ConversionValidationException.class, () -> service.execute(request));
    }

    @Test
    void createsJobAndStoresPayloadWithoutRawCodeInRequestJson() {
        final InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        final InMemoryObjectStore objectStore = new InMemoryObjectStore();
        final InMemoryQueuePublisher queuePublisher = new InMemoryQueuePublisher();
        final var service = new CreateConversionJobService(jobRepository, objectStore, queuePublisher, new FixedConfig());

        final CreateConversionRequestModel request = new CreateConversionRequestModel(
            "cobol",
            "java",
            "21",
            "hexagonal",
            "IF A = B THEN",
            Map.of("mcpBaseUrl", "https://override.example.com")
        );

        final CreateConversionResponseModel response = service.execute(request);

        assertNotNull(response.jobId());
        assertEquals("PENDING", response.status());
        assertEquals("/conversions/" + response.jobId(), response.pollUrl());
        assertNotNull(jobRepository.saved);
        assertEquals(response.jobId(), jobRepository.saved.jobId());
        assertTrue(objectStore.jsonByKey.containsKey("conversions/" + response.jobId() + "/request.json"));
        assertTrue(objectStore.textByKey.containsKey("conversions/" + response.jobId() + "/input.cob"));
        assertEquals("IF A = B THEN", objectStore.textByKey.get("conversions/" + response.jobId() + "/input.cob"));

        final Map<String, Object> storedRequest = objectStore.jsonByKey.get("conversions/" + response.jobId() + "/request.json");
        assertFalse(storedRequest.containsKey("code"));
        assertEquals("cobol", storedRequest.get("languageSelected"));
        assertNotNull(queuePublisher.lastMessage);
        assertEquals("convert_code", queuePublisher.lastMessage.mcp().tool());
        assertEquals("https://override.example.com", queuePublisher.lastMessage.mcp().baseUrl());
    }

    @Test
    void marksJobFailedWhenSqsPublishFails() {
        final TrackingJobRepository jobRepository = new TrackingJobRepository();
        final InMemoryObjectStore objectStore = new InMemoryObjectStore();
        final QueuePublisherPort failingPublisher = message -> {
            throw new IllegalStateException("sqs down");
        };
        final var service = new CreateConversionJobService(jobRepository, objectStore, failingPublisher, new FixedConfig());

        final CreateConversionRequestModel request = new CreateConversionRequestModel(
            "cobol",
            "java",
            "21",
            "hexagonal",
            "IF A = B THEN",
            Map.of()
        );

        assertThrows(IllegalStateException.class, () -> service.execute(request));
        assertEquals(2, jobRepository.savedJobs.size());
        assertEquals("PENDING", jobRepository.savedJobs.get(0).status().name());
        assertEquals("FAILED", jobRepository.savedJobs.get(1).status().name());
    }

    private static final class InMemoryJobRepository implements JobRepositoryPort {
        private ConversionJob saved;

        @Override
        public void save(ConversionJob job) {
            this.saved = job;
        }

        @Override
        public Optional<ConversionJob> findByJobId(String jobId) {
            return Optional.empty();
        }
    }

    private static final class TrackingJobRepository implements JobRepositoryPort {
        private final List<ConversionJob> savedJobs = new java.util.ArrayList<>();

        @Override
        public void save(ConversionJob job) {
            savedJobs.add(job);
        }

        @Override
        public Optional<ConversionJob> findByJobId(String jobId) {
            return Optional.empty();
        }
    }

    private static final class InMemoryObjectStore implements ObjectStorePort {
        private final Map<String, Map<String, Object>> jsonByKey = new HashMap<>();
        private final Map<String, String> textByKey = new HashMap<>();

        @Override
        public void putJson(String key, Map<String, Object> payload) {
            jsonByKey.put(key, Map.copyOf(payload));
        }

        @Override
        public void putText(String key, String payload) {
            textByKey.put(key, payload);
        }
    }

    private static final class InMemoryQueuePublisher implements QueuePublisherPort {
        private ConversionMessage lastMessage;

        @Override
        public void publish(ConversionMessage message) {
            this.lastMessage = message;
        }
    }

    private record FixedConfig() implements ConversionConfigPort {
        @Override
        public String ddbTable() {
            return "kata-converter-jobs-qa";
        }

        @Override
        public String artifactBucket() {
            return "kata-converter-artifacts-qa";
        }

        @Override
        public String queueUrl() {
            return "https://sqs.us-east-1.amazonaws.com/827164363053/kata-converter-jobs-qa";
        }

        @Override
        public String mcpBaseUrl() {
            return "";
        }

        @Override
        public int presignTtlMinutes() {
            return 15;
        }

        @Override
        public int jobTtlDays() {
            return 7;
        }
    }
}
