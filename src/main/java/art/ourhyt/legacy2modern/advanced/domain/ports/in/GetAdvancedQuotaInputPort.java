package art.ourhyt.legacy2modern.advanced.domain.ports.in;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;

public interface GetAdvancedQuotaInputPort {
    AdvancedQuotaResponseModel execute(String authorizationHeader);
}
