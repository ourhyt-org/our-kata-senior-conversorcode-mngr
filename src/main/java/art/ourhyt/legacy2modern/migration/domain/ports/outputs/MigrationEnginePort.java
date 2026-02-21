package art.ourhyt.legacy2modern.migration.domain.ports.outputs;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.domain.model.MigrationResult;

public interface MigrationEnginePort {
    MigrationResult migrate(MigrateRequestModel request);
}
