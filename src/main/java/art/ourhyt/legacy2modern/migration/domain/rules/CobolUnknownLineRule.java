package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.ArrayList;
import java.util.List;

public final class CobolUnknownLineRule implements Rule {
    @Override
    public String id() {
        return "RULE_UNKNOWN";
    }

    @Override
    public String name() {
        return "Unknown COBOL line -> comment";
    }

    @Override
    public String description() {
        return "Keeps unknown COBOL syntax as comments";
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
            if (shouldWrapAsUnknown(line)) {
                updated.add("// TODO: UNMAPPED: " + line.trim());
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }

    private boolean shouldWrapAsUnknown(String line) {
        final String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (trimmed.startsWith("//") || trimmed.equals("}") || trimmed.equals("} else {") || trimmed.startsWith("if (")) {
            return false;
        }
        if (trimmed.contains("=") || trimmed.startsWith("System.out.println") || trimmed.startsWith("console.log") || trimmed.startsWith("print(") || trimmed.startsWith("fmt.Println") || trimmed.equals("System.exit(0);") || trimmed.equals("return;")) {
            return false;
        }
        return true;
    }
}
