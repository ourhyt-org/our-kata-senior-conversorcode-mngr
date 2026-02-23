package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionRequestModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;
import art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest.CreateConversionHttpRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class AdvancedHttpMapper {
    public AdvancedCreateConversionRequestModel toApplication(CreateConversionHttpRequest request) {
        return new AdvancedCreateConversionRequestModel(
            request.languageSelected(),
            request.languageTarget(),
            request.version(),
            request.typeArchitected(),
            request.code(),
            request.options() == null ? Map.of() : request.options()
        );
    }

    public AdvancedCreateConversionHttpResponse fromApplication(AdvancedCreateConversionResponseModel response) {
        return new AdvancedCreateConversionHttpResponse(response.jobId(), response.status(), response.pollUrl(), response.quota());
    }

    public AdvancedQuotaHttpResponse fromApplication(AdvancedQuotaResponseModel response) {
        return new AdvancedQuotaHttpResponse(response.quota());
    }
}
