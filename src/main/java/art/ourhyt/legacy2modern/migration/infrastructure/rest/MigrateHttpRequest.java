package art.ourhyt.legacy2modern.migration.infrastructure.rest;

public record MigrateHttpRequest(String sourceLanguage, String targetLanguage, String targetVersion, String code) {
}
