package art.ourhyt.legacy2modern.migration.domain;

public record MigrationContext(SourceLanguage sourceLanguage, TargetLanguage targetLanguage, String targetVersion) {
}
