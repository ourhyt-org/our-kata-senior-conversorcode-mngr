package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.TargetLanguage;

public record MigrateRequestModel(SourceLanguage sourceLanguage, TargetLanguage targetLanguage, String targetVersion, String code) {
}
