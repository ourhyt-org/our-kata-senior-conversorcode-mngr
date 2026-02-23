package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

public record GetConversionStatusHttpResponse(
    String jobId,
    String status,
    String createdAt,
    String startedAt,
    String finishedAt,
    String outputS3Key,
    String reportS3Key,
    String downloadUrl,
    String reportUrl,
    String errorMessage
) {
}
