package art.ourhyt.legacy2modern.conversions.domain.ports.in;

import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;

public interface GetConversionFilesInputPort {
    GetConversionFilesResponseModel execute(GetConversionFilesRequestModel request);
}
