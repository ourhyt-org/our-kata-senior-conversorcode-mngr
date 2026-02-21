package art.ourhyt.legacy2modern.migration.domain.model;

public record MigrationContext(SourceLanguage sourceLanguage, TargetLanguage targetLanguage, String targetVersion) {
}
