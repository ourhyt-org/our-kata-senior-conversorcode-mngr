package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MigrateHttpRequest(
    @NotNull @NotBlank String sourceLanguage,
    @NotNull @NotBlank String targetLanguage,
    String targetVersion,
    @NotNull @NotBlank String code
) {
}
