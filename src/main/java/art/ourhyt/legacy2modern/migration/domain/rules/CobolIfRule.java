package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CobolIfRule implements Rule {
    private static final Pattern PATTERN = Pattern.compile("^\\s*IF\\s+(.+?)\\s+THEN\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "RULE_IF";
    }

    @Override
    public String name() {
        return "COBOL IF -> if() {";
    }

    @Override
    public String description() {
        return "Transforms COBOL IF THEN into modern if block";
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
            final Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                updated.add("if (" + matcher.group(1).trim() + ") {");
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }
}
