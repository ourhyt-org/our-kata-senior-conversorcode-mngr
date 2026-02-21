package art.ourhyt.legacy2modern.migration.domain;

import java.util.List;

public record RuleResult(List<String> updatedLines, int matchesCount, List<Integer> lineNumbers) {
}
