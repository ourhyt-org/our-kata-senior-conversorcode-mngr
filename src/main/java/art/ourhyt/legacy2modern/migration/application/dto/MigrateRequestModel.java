package art.ourhyt.legacy2modern.migration.application.dto;

import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;

public record MigrateRequestModel(SourceLanguage sourceLanguage, TargetLanguage targetLanguage, String targetVersion, String code) {
}
