package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.List;

public interface Rule {
    String id();

    String name();

    String description();

    RuleResult apply(List<String> lines, MigrationContext context);
}
