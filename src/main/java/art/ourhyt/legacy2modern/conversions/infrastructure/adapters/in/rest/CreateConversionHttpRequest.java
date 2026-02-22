package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateConversionHttpRequest(
    @NotBlank String languageSelected,
    @NotBlank String languageTarget,
    @NotBlank String version,
    @NotBlank String typeArchitected,
    @NotBlank String code,
    Map<String, Object> options
) {
}
