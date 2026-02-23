package art.ourhyt.legacy2modern.conversions.domain.ports.in;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;

public interface CreateConversionJobInputPort {
    CreateConversionResponseModel execute(CreateConversionRequestModel request);
}
