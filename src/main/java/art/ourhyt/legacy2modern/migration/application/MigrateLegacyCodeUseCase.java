package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.MigrationResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MigrateLegacyCodeUseCase {
    private final MigrationEnginePort migrationEngine;
    private final PayloadPolicyPort payloadPolicy;

    public MigrateLegacyCodeUseCase(MigrationEnginePort migrationEngine, PayloadPolicyPort payloadPolicy) {
        this.migrationEngine = migrationEngine;
        this.payloadPolicy = payloadPolicy;
    }

    public MigrateResponseModel execute(MigrateRequestModel request) {
        validateRequest(request);
        final MigrationResult result = migrationEngine.migrate(request);
        return new MigrateResponseModel(result.outputCode(), result.report());
    }

    private void validateRequest(MigrateRequestModel request) {
        final List<String> details = new ArrayList<>();
        if (request == null) {
            throw new ValidationException("VALIDATION_ERROR", "Request body is required", List.of("body must not be null"));
        }
        if (request.sourceLanguage() == null) {
            details.add("sourceLanguage is required");
        }
        if (request.targetLanguage() == null) {
            details.add("targetLanguage is required");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            details.add("code is required");
        }
        if (!details.isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid migration request", details);
        }

        final int bytes = request.code().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > payloadPolicy.maxPayloadBytes()) {
            throw new PayloadTooLargeException("PAYLOAD_TOO_LARGE", "Payload exceeds max size", List.of("maxBytes=" + payloadPolicy.maxPayloadBytes()));
        }

        final int lineCount = request.code().split("\\R", -1).length;
        if (lineCount > payloadPolicy.maxLines()) {
            throw new PayloadTooLargeException("PAYLOAD_TOO_LARGE", "Payload exceeds max lines", List.of("maxLines=" + payloadPolicy.maxLines()));
        }
    }
}
