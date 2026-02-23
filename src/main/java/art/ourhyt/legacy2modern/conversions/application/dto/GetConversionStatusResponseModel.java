package art.ourhyt.legacy2modern.conversions.application.dto;

public record GetConversionStatusResponseModel(
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
