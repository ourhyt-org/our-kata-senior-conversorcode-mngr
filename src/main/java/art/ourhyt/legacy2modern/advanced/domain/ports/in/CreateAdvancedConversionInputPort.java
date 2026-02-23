package art.ourhyt.legacy2modern.advanced.domain.ports.in;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionRequestModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;

public interface CreateAdvancedConversionInputPort {
    AdvancedCreateConversionResponseModel execute(String authorizationHeader, AdvancedCreateConversionRequestModel request);
}
