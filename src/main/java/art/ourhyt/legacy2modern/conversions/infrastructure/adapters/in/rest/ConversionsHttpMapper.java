package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ConversionsHttpMapper {
    public CreateConversionRequestModel toApplication(CreateConversionHttpRequest request) {
        return new CreateConversionRequestModel(
            request.languageSelected(),
            request.languageTarget(),
            request.version(),
            request.typeArchitected(),
            request.code(),
            request.options() == null ? Map.of() : request.options()
        );
    }

    public CreateConversionHttpResponse fromApplication(CreateConversionResponseModel response) {
        return new CreateConversionHttpResponse(response.jobId(), response.status(), response.pollUrl());
    }

    public GetConversionStatusHttpResponse fromApplication(GetConversionStatusResponseModel response) {
        return new GetConversionStatusHttpResponse(
            response.jobId(),
            response.status(),
            response.createdAt(),
            response.startedAt(),
            response.finishedAt(),
            response.outputS3Key(),
            response.reportS3Key(),
            response.downloadUrl(),
            response.reportUrl(),
            response.errorMessage()
        );
    }

    public GetConversionFilesHttpResponse fromApplication(GetConversionFilesResponseModel response) {
        return new GetConversionFilesHttpResponse(
            response.jobId(),
            response.status(),
            response.outputS3Key(),
            response.zipSizeBytes(),
            response.defaultFile(),
            response.recommendedFiles(),
            response.manifest().stream().map(m -> new ManifestEntryHttpResponse(m.path(), m.sizeBytes(), m.isText())).toList(),
            response.files().stream().map(f -> new FileContentHttpResponse(f.path(), f.content())).toList(),
            response.skipped().stream().map(s -> new SkippedFileHttpResponse(s.path(), s.reason())).toList()
        );
    }
}
