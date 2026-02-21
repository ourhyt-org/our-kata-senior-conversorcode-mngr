package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.MigrationReport;

public record MigrateResponseModel(String outputCode, MigrationReport report) {
}
