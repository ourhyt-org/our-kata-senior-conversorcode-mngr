package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CobolDisplayRule implements Rule {
    private static final Pattern PATTERN = Pattern.compile("^\\s*DISPLAY\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "RULE_DISPLAY";
    }

    @Override
    public String name() {
        return "COBOL DISPLAY -> print";
    }

    @Override
    public String description() {
        return "Transforms COBOL DISPLAY to target output print function";
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
                final String payload = normalizeLiteral(matcher.group(1));
                updated.add(renderDisplay(payload, context.targetLanguage()));
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }

    private String normalizeLiteral(String raw) {
        final String trimmed = raw.trim();
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return '"' + trimmed.substring(1, trimmed.length() - 1) + '"';
        }
        return trimmed;
    }

    private String renderDisplay(String payload, TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA -> "System.out.println(" + payload + ");";
            case NODE -> "console.log(" + payload + ");";
            case PYTHON -> "print(" + payload + ")";
            case GO -> "fmt.Println(" + payload + ")";
        };
    }
}
