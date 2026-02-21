package art.ourhyt.legacy2modern.migration.application.services;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.application.exceptions.PayloadTooLargeException;
import art.ourhyt.legacy2modern.migration.application.exceptions.ValidationException;
import art.ourhyt.legacy2modern.migration.domain.model.MigrationResult;
import art.ourhyt.legacy2modern.migration.domain.ports.in.MigrateLegacyCodeInputPort;
import art.ourhyt.legacy2modern.migration.domain.ports.outputs.MigrationEnginePort;
import art.ourhyt.legacy2modern.migration.domain.ports.outputs.PayloadPolicyPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public final class MigrateLegacyCodeService implements MigrateLegacyCodeInputPort {
    private final MigrationEnginePort migrationEngine;
    private final PayloadPolicyPort payloadPolicy;

    @Inject
    public MigrateLegacyCodeService(MigrationEnginePort migrationEngine, PayloadPolicyPort payloadPolicy) {
        this.migrationEngine = migrationEngine;
        this.payloadPolicy = payloadPolicy;
    }

    @Override
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
