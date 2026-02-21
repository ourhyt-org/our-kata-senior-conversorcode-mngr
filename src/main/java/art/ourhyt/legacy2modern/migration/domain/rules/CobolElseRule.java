package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.ArrayList;
import java.util.List;

public final class CobolElseRule implements Rule {
    @Override
    public String id() {
        return "RULE_ELSE";
    }

    @Override
    public String name() {
        return "COBOL ELSE -> } else {";
    }

    @Override
    public String description() {
        return "Transforms COBOL ELSE into modern else block";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.COBOL) {
            return new RuleResult(lines, 0, List.of());
        }
        final List<String> updated = new ArrayList<>(lines.size());
        final List<Integer> lineNumbers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.trim().equalsIgnoreCase("ELSE")) {
                updated.add("} else {");
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
