package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.ArrayList;
import java.util.List;

public final class CobolCommentRule implements Rule {
    @Override
    public String id() {
        return "RULE_COMMENT";
    }

    @Override
    public String name() {
        return "COBOL comments -> //";
    }

    @Override
    public String description() {
        return "Normalizes COBOL comments to double-slash comments";
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
            final String trimmed = line.trim();
            if (trimmed.startsWith("*>") || trimmed.startsWith("*")) {
                final String normalized = trimmed.startsWith("*>") ? trimmed.substring(2).trim() : trimmed.substring(1).trim();
                updated.add("// " + normalized);
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
