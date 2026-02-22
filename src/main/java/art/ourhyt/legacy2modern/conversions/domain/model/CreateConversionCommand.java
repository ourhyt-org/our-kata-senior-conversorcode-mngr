package art.ourhyt.legacy2modern.conversions.domain.model;

import java.util.Map;

public record CreateConversionCommand(
    String languageSelected,
    String languageTarget,
    String version,
    String typeArchitected,
    String code,
    Map<String, Object> options
) {
}
