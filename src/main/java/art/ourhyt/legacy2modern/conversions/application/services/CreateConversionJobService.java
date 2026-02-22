package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionValidationException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionMessage;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ObjectStorePort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.QueuePublisherPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CreateConversionJobService implements CreateConversionJobInputPort {
    private static final Logger LOG = Logger.getLogger(CreateConversionJobService.class);
    private static final Set<String> ALLOWED_SOURCE = Set.of("cobol", "delphi");
    private static final Set<String> ALLOWED_TARGET = Set.of("go", "java", "python", "node");
    private static final Set<String> ALLOWED_ARCH = Set.of("clean", "hexagonal", "layered", "minimal");

    private final JobRepositoryPort jobRepository;
    private final ObjectStorePort objectStore;
    private final QueuePublisherPort queuePublisher;
    private final ConversionConfigPort config;

    @Inject
    public CreateConversionJobService(JobRepositoryPort jobRepository, ObjectStorePort objectStore, QueuePublisherPort queuePublisher, ConversionConfigPort config) {
        this.jobRepository = jobRepository;
        this.objectStore = objectStore;
        this.queuePublisher = queuePublisher;
        this.config = config;
    }

    @Override
    public CreateConversionResponseModel execute(CreateConversionRequestModel request) {
        final ValidatedRequest validated = validate(request);
        final String jobId = UUID.randomUUID().toString();
        final String createdAt = Instant.now().toString();
        final String requestS3Key = "conversions/" + jobId + "/request.json";
        final String codeS3Key = "conversions/" + jobId + "/" + inputFileName(validated.languageSelected);

        final Map<String, Object> requestJson = new LinkedHashMap<>();
        requestJson.put("languageSelected", validated.languageSelected);
        requestJson.put("languageTarget", validated.languageTarget);
        requestJson.put("version", validated.version);
        requestJson.put("typeArchitected", validated.typeArchitected);
        requestJson.put("options", validated.options);

        objectStore.putJson(requestS3Key, requestJson);
        objectStore.putText(codeS3Key, validated.code);

        final Long ttl = Instant.now().plus(config.jobTtlDays(), ChronoUnit.DAYS).getEpochSecond();
        final ConversionJob job = new ConversionJob(
            jobId,
            JobStatus.PENDING,
            createdAt,
            null,
            null,
            requestS3Key,
            codeS3Key,
            null,
            null,
            null,
            ttl
        );
        jobRepository.save(job);

        final ConversionMessage message = new ConversionMessage(
            jobId,
            config.artifactBucket(),
            requestS3Key,
            config.artifactBucket(),
            codeS3Key,
            new ConversionMessage.McpMessage(resolveMcpBaseUrl(validated.options), "convert_code")
        );
        queuePublisher.publish(message);

        final int codeSizeBytes = validated.code.getBytes(StandardCharsets.UTF_8).length;
        final String codeHash = sha256(validated.code);
        LOG.infov("jobId={0} event=conversion_enqueued source={1} target={2} codeBytes={3} codeSha256={4}", jobId, validated.languageSelected, validated.languageTarget, codeSizeBytes, codeHash);

        return new CreateConversionResponseModel(jobId, JobStatus.PENDING.name(), "/conversions/" + jobId);
    }

    private ValidatedRequest validate(CreateConversionRequestModel request) {
        final List<String> details = new ArrayList<>();
        if (request == null) {
            throw new ConversionValidationException("VALIDATION_ERROR", "Invalid conversion request", List.of("request body is required"));
        }

        final String source = normalize(request.languageSelected());
        final String target = normalize(request.languageTarget());
        final String version = normalize(request.version());
        final String typeArchitected = normalize(request.typeArchitected());
        final String code = request.code();

        if (source == null || !ALLOWED_SOURCE.contains(source)) {
            details.add("languageSelected must be one of cobol|delphi");
        }
        if (target == null || !ALLOWED_TARGET.contains(target)) {
            details.add("languageTarget must be one of go|java|python|node");
        }
        if (version == null) {
            details.add("version is required");
        }
        if (typeArchitected == null || !ALLOWED_ARCH.contains(typeArchitected)) {
            details.add("typeArchitected must be one of clean|hexagonal|layered|minimal");
        }
        if (code == null || code.trim().isEmpty()) {
            details.add("code is required");
        }

        if (!details.isEmpty()) {
            throw new ConversionValidationException("VALIDATION_ERROR", "Invalid conversion request", details);
        }

        final Map<String, Object> options = request.options() == null ? Map.of() : Map.copyOf(request.options());
        return new ValidatedRequest(source, target, version, typeArchitected, code, options);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String inputFileName(String languageSelected) {
        return switch (languageSelected) {
            case "cobol" -> "input.cob";
            case "delphi" -> "input.pas";
            default -> "input.txt";
        };
    }

    private String resolveMcpBaseUrl(Map<String, Object> options) {
        final Object value = options.get("mcpBaseUrl");
        if (value instanceof String stringValue && !stringValue.trim().isEmpty()) {
            return stringValue.trim();
        }
        final String fallback = config.mcpBaseUrl();
        if (fallback == null || fallback.trim().isEmpty()) {
            return null;
        }
        return fallback.trim();
    }

    private String sha256(String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return "sha256-unavailable";
        }
    }

    private record ValidatedRequest(
        String languageSelected,
        String languageTarget,
        String version,
        String typeArchitected,
        String code,
        Map<String, Object> options
    ) {
    }
}
