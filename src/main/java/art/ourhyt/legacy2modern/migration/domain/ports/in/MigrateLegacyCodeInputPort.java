package art.ourhyt.legacy2modern.migration.domain.ports.in;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;

public interface MigrateLegacyCodeInputPort {
    MigrateResponseModel execute(MigrateRequestModel request);
}
