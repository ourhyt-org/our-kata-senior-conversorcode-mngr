package art.ourhyt.legacy2modern.migration.domain;

import java.util.ArrayList;
import java.util.List;

public final class CobolEndIfRule implements Rule {
    @Override
    public String id() {
        return "RULE_END_IF";
    }

    @Override
    public String name() {
        return "COBOL END-IF -> }";
    }

    @Override
    public String description() {
        return "Transforms COBOL END-IF into closing brace";
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
            if (line.trim().equalsIgnoreCase("END-IF")) {
                updated.add("}");
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
