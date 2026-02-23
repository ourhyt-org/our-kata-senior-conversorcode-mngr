package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.advanced.application.dto.QuotaHttpModel;

public record AdvancedCreateConversionHttpResponse(
    String jobId,
    String status,
    String pollUrl,
    QuotaHttpModel quota
) {
}
