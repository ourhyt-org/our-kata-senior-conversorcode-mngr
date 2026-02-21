package art.ourhyt.legacy2modern.migration.application.dto;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationReport;

public record MigrateResponseModel(String outputCode, MigrationReport report) {
}
