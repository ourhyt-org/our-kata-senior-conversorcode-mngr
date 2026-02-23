package art.ourhyt.legacy2modern.advanced.application.dto;

import java.util.Map;

public record AdvancedCreateConversionRequestModel(
    String languageSelected,
    String languageTarget,
    String version,
    String typeArchitected,
    String code,
    Map<String, Object> options
) {
}
