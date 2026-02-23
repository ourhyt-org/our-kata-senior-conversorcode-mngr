package art.ourhyt.legacy2modern.conversions.domain.model;

public record ConversionJob(
    String jobId,
    JobStatus status,
    String createdAt,
    String startedAt,
    String finishedAt,
    String requestS3Key,
    String codeS3Key,
    String outputS3Key,
    String reportS3Key,
    String errorMessage,
    Long ttl
) {
}
