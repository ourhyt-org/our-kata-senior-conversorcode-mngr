package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationReport;

public record MigrateHttpResponse(String outputCode, MigrationReport report) {
}
