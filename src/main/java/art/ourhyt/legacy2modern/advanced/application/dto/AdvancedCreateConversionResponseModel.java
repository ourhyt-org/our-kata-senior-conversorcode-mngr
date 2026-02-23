package art.ourhyt.legacy2modern.advanced.application.dto;

public record AdvancedCreateConversionResponseModel(
    String jobId,
    String status,
    String pollUrl,
    QuotaHttpModel quota
) {
}
