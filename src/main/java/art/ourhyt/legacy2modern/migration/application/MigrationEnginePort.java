package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.MigrationResult;

public interface MigrationEnginePort {
    MigrationResult migrate(MigrateRequestModel request);
}
