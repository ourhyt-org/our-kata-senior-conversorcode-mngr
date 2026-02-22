package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.UrlSignerPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetConversionStatusServiceTest {
    @Test
    void throwsWhenNotFound() {
        final var service = new GetConversionStatusService(new RepositoryStub(Optional.empty()), (key, ttl) -> "", new FixedConfig());
        assertThrows(ConversionNotFoundException.class, () -> service.execute("missing"));
    }

    @Test
    void returnsPendingShape() {
        final ConversionJob job = new ConversionJob("j1", JobStatus.PENDING, "2026-02-22T10:00:00Z", null, null, "req", "code", null, null, null, null);
        final var service = new GetConversionStatusService(new RepositoryStub(Optional.of(job)), (key, ttl) -> "", new FixedConfig());

        final GetConversionStatusResponseModel response = service.execute("j1");

        assertEquals("PENDING", response.status());
        assertEquals("j1", response.jobId());
        assertNull(response.downloadUrl());
    }

    @Test
    void returnsFinishedWithPresignedUrls() {
        final ConversionJob job = new ConversionJob("j2", JobStatus.FINISHED, "2026-02-22T10:00:00Z", "2026-02-22T10:01:00Z", "2026-02-22T10:02:00Z", "req", "code", "conversions/j2/output.zip", "conversions/j2/report.json", null, null);
        final UrlSignerPort signer = new UrlSignerPort() {
            @Override
            public String presignGet(String key, Duration ttl) {
                return "https://signed.example.com/" + key;
            }
        };
        final var service = new GetConversionStatusService(new RepositoryStub(Optional.of(job)), signer, new FixedConfig());

        final GetConversionStatusResponseModel response = service.execute("j2");

        assertEquals("FINISHED", response.status());
        assertNotNull(response.downloadUrl());
        assertNotNull(response.reportUrl());
        assertEquals("conversions/j2/output.zip", response.outputS3Key());
    }

    @Test
    void returnsFailedShape() {
        final ConversionJob job = new ConversionJob("j3", JobStatus.FAILED, "2026-02-22T10:00:00Z", "2026-02-22T10:01:00Z", "2026-02-22T10:02:00Z", "req", "code", null, null, "boom", null);
        final var service = new GetConversionStatusService(new RepositoryStub(Optional.of(job)), (key, ttl) -> "", new FixedConfig());

        final GetConversionStatusResponseModel response = service.execute("j3");

        assertEquals("FAILED", response.status());
        assertEquals("boom", response.errorMessage());
        assertEquals("2026-02-22T10:02:00Z", response.finishedAt());
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

    private record RepositoryStub(Optional<ConversionJob> found) implements JobRepositoryPort {
        @Override
        public void save(ConversionJob job) {
        }

        @Override
        public Optional<ConversionJob> findByJobId(String jobId) {
            return found;
        }
    }
}
