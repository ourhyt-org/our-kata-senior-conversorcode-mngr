package art.ourhyt.legacy2modern.conversions.application.dto;

import java.util.Map;

public record CreateConversionRequestModel(
    String languageSelected,
    String languageTarget,
    String version,
    String typeArchitected,
    String code,
    Map<String, Object> options
) {
}
