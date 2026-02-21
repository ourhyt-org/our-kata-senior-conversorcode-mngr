package art.ourhyt.legacy2modern.migration.domain.model;

import java.util.List;

public record AppliedRule(String id, String name, String description, int matches, List<Integer> lineNumbers) {
}
