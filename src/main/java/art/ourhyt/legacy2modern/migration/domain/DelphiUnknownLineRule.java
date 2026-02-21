package art.ourhyt.legacy2modern.migration.domain;

import java.util.ArrayList;
import java.util.List;

public final class DelphiUnknownLineRule implements Rule {
    @Override
    public String id() {
        return "RULE_DELPHI_UNKNOWN";
    }

    @Override
    public String name() {
        return "Unknown DELPHI line -> comment";
    }

    @Override
    public String description() {
        return "Keeps unsupported Delphi syntax as comments";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.DELPHI) {
            return new RuleResult(lines, 0, List.of());
        }
        final List<String> updated = new ArrayList<>(lines.size());
        final List<Integer> lineNumbers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.equals("{") || trimmed.equals("}")) {
                updated.add(line);
            } else {
                updated.add("// TODO: UNMAPPED: " + trimmed);
                lineNumbers.add(i + 1);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
