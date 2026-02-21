package art.ourhyt.legacy2modern.migration.domain;

import java.util.List;

public interface Rule {
    String id();

    String name();

    String description();

    RuleResult apply(List<String> lines, MigrationContext context);
}
