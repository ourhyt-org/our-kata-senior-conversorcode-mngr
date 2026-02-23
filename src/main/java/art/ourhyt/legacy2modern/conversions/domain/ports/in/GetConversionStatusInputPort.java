package art.ourhyt.legacy2modern.conversions.domain.ports.in;

import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;

public interface GetConversionStatusInputPort {
    GetConversionStatusResponseModel execute(String jobId);
}
