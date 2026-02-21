package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.application.exceptions.ValidationException;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MigrateHttpMapper {
    public MigrateRequestModel toApplication(MigrateHttpRequest request) {
        if (request == null) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid migration request", List.of("body must not be null"));
        }

        return new MigrateRequestModel(
            parseSourceLanguage(request.sourceLanguage()),
            parseTargetLanguage(request.targetLanguage()),
            request.targetVersion(),
            request.code()
        );
    }

    public MigrateHttpResponse fromApplication(MigrateResponseModel response) {
        return new MigrateHttpResponse(response.outputCode(), response.report());
    }

    private SourceLanguage parseSourceLanguage(String value) {
        try {
            return value == null ? null : SourceLanguage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid migration request", List.of("sourceLanguage must be COBOL or DELPHI"));
        }
    }

    private TargetLanguage parseTargetLanguage(String value) {
        try {
            return value == null ? null : TargetLanguage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("VALIDATION_ERROR", "Invalid migration request", List.of("targetLanguage must be JAVA, NODE, PYTHON or GO"));
        }
    }
}
