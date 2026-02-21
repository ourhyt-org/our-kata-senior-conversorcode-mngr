package art.ourhyt.legacy2modern.migration.domain.model;

import java.util.List;

public record MigrationReport(List<AppliedRule> appliedRules, List<Warning> warnings) {
}
