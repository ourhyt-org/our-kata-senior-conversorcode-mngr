package art.ourhyt.legacy2modern.migration.domain;

import java.util.ArrayList;
import java.util.List;

public final class DelphiBeginEndRule implements Rule {
    @Override
    public String id() {
        return "RULE_DELPHI_BEGIN_END";
    }

    @Override
    public String name() {
        return "DELPHI begin/end -> braces";
    }

    @Override
    public String description() {
        return "Converts Delphi begin/end tokens to braces";
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
            if (trimmed.equalsIgnoreCase("begin")) {
                updated.add("{");
                lineNumbers.add(i + 1);
            } else if (trimmed.equalsIgnoreCase("end") || trimmed.equalsIgnoreCase("end;")) {
                updated.add("}");
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
