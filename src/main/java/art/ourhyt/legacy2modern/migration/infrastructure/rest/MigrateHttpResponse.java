package art.ourhyt.legacy2modern.migration.infrastructure.rest;

import art.ourhyt.legacy2modern.migration.domain.MigrationReport;

public record MigrateHttpResponse(String outputCode, MigrationReport report) {
}
