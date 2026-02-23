package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotReadyException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ObjectStorePort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetConversionFilesServiceTest {
    @Test
    void returnsNotFoundWhenJobMissing() {
        final var service = new GetConversionFilesService(new Repo(Optional.empty()), new Store(Map.of()), new FixedConfig());

        assertThrows(ConversionNotFoundException.class, () -> service.execute(request("job-1", false, List.of(), 30, 300000, 200000)));
    }

    @Test
    void returnsNotReadyWhenJobIsNotFinished() {
        final ConversionJob job = new ConversionJob("job-1", JobStatus.RUNNING, "2026-01-01T00:00:00Z", null, null, "a", "b", "conversions/job-1/output.zip", null, null, null);
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of()), new FixedConfig());

        assertThrows(ConversionNotReadyException.class, () -> service.execute(request("job-1", false, List.of(), 30, 300000, 200000)));
    }

    @Test
    void buildsManifestAndDefaultAndRecommended() throws Exception {
        final byte[] zip = zipOf(Map.of(
            "cmd/app/main.go", "package main\nfunc main(){}\n",
            "README.md", "hello",
            "assets/logo.png", "PNG"
        ));
        final ConversionJob job = finishedJob("job-1", "conversions/job-1/output.zip");
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of("conversions/job-1/output.zip", zip)), new FixedConfig());

        final GetConversionFilesResponseModel response = service.execute(request("job-1", false, List.of(), 30, 300000, 200000));

        assertEquals("job-1", response.jobId());
        assertEquals("FINISHED", response.status());
        assertEquals("cmd/app/main.go", response.defaultFile());
        assertFalse(response.recommendedFiles().isEmpty());
        assertEquals("cmd/app/main.go", response.recommendedFiles().getFirst());
        assertTrue(response.zipSizeBytes() > 0);
        assertTrue(response.manifest().stream().anyMatch(m -> m.path().equals("cmd/app/main.go")));
        assertTrue(response.files().isEmpty());
    }

    @Test
    void includesSpecificContentWhenRequested() throws Exception {
        final byte[] zip = zipOf(Map.of(
            "cmd/app/main.go", "package main\nfunc main(){}\n",
            "README.md", "hello"
        ));
        final ConversionJob job = finishedJob("job-2", "conversions/job-2/output.zip");
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of("conversions/job-2/output.zip", zip)), new FixedConfig());

        final GetConversionFilesResponseModel response = service.execute(request("job-2", true, List.of("cmd/app/main.go"), 30, 300000, 200000));

        assertEquals(1, response.files().size());
        assertEquals("cmd/app/main.go", response.files().getFirst().path());
        assertTrue(response.files().getFirst().content().contains("package main"));
    }

    @Test
    void defaultsToDefaultFileWhenIncludeContentWithoutPaths() throws Exception {
        final byte[] zip = zipOf(Map.of("main.py", "print('ok')\n"));
        final ConversionJob job = finishedJob("job-3", "conversions/job-3/output.zip");
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of("conversions/job-3/output.zip", zip)), new FixedConfig());

        final GetConversionFilesResponseModel response = service.execute(request("job-3", true, List.of(), 30, 300000, 200000));

        assertEquals("main.py", response.defaultFile());
        assertEquals(1, response.files().size());
        assertEquals("main.py", response.files().getFirst().path());
    }

    @Test
    void enforcesPerFileAndTotalLimitsAndSkipsUnsupported() throws Exception {
        final String large = "x".repeat(120);
        final byte[] zip = zipOf(Map.of(
            "main.go", large,
            "README.md", "1234567890",
            "bin/app", "abc"
        ));
        final ConversionJob job = finishedJob("job-4", "conversions/job-4/output.zip");
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of("conversions/job-4/output.zip", zip)), new FixedConfig());

        final GetConversionFilesResponseModel response = service.execute(request("job-4", true, List.of("main.go", "README.md", "bin/app", "missing.txt"), 1, 20, 50));

        assertTrue(response.skipped().stream().anyMatch(s -> s.path().equals("main.go") && s.reason().equals("file_too_large")));
        assertTrue(response.skipped().stream().anyMatch(s -> s.path().equals("missing.txt") && s.reason().equals("path_not_found")));
        assertTrue(response.skipped().stream().anyMatch(s -> s.path().equals("bin/app") && s.reason().equals("unsupported_extension")));
        assertTrue(response.files().stream().allMatch(f -> f.path().equals("README.md")));
    }

    @Test
    void skipsNonUtf8Content() throws Exception {
        final byte[] zip = zipWithRawEntry("main.go", new byte[]{(byte) 0xC3, (byte) 0x28});
        final ConversionJob job = finishedJob("job-5", "conversions/job-5/output.zip");
        final var service = new GetConversionFilesService(new Repo(Optional.of(job)), new Store(Map.of("conversions/job-5/output.zip", zip)), new FixedConfig());

        final GetConversionFilesResponseModel response = service.execute(request("job-5", true, List.of("main.go"), 30, 300000, 200000));

        assertTrue(response.files().isEmpty());
        assertTrue(response.skipped().stream().anyMatch(s -> s.path().equals("main.go") && s.reason().equals("binary_or_non_utf8")));
    }

    private GetConversionFilesRequestModel request(String jobId, boolean includeContent, List<String> paths, int maxFiles, int maxTotalBytes, int maxFileBytes) {
        return new GetConversionFilesRequestModel(jobId, includeContent, paths, maxFiles, maxTotalBytes, maxFileBytes);
    }

    private ConversionJob finishedJob(String jobId, String outputS3Key) {
        return new ConversionJob(jobId, JobStatus.FINISHED, "2026-01-01T00:00:00Z", "2026-01-01T00:01:00Z", "2026-01-01T00:02:00Z", "req", "code", outputS3Key, "report", null, null);
    }

    private byte[] zipOf(Map<String, String> entries) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private byte[] zipWithRawEntry(String path, byte[] bytes) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(path));
            zos.write(bytes);
            zos.closeEntry();
        }
        return out.toByteArray();
    }

    private record Repo(Optional<ConversionJob> found) implements JobRepositoryPort {
        @Override
        public void save(ConversionJob job) {
        }

        @Override
        public Optional<ConversionJob> findByJobId(String jobId) {
            return found;
        }
    }

    private record Store(Map<String, byte[]> bytesByKey) implements ObjectStorePort {
        @Override
        public void putJson(String key, Map<String, Object> payload) {
        }

        @Override
        public void putText(String key, String payload) {
        }

        @Override
        public byte[] getObjectBytes(String bucket, String key) {
            final byte[] bytes = bytesByKey.get(key);
            if (bytes == null) {
                throw new IllegalStateException("Missing test object for key " + key);
            }
            return bytes;
        }
    }

    private record FixedConfig() implements ConversionConfigPort {
        @Override
        public String ddbTable() {
            return "table";
        }

        @Override
        public String artifactBucket() {
            return "bucket";
        }

        @Override
        public String queueUrl() {
            return "queue";
        }

        @Override
        public String mcpBaseUrl() {
            return null;
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
