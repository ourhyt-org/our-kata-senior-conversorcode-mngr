package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.LegacyLineNormalizer;
import art.ourhyt.legacy2modern.migration.domain.rules.support.Patterns;
import art.ourhyt.legacy2modern.migration.domain.rules.support.RuleSupport;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CobolIfRule implements Rule {
    private static final Pattern IF_PATTERN =
            Patterns.cobolStmt("IF\\s+(.+?)(?:\\s+THEN\\b)?");
    private static final Pattern SIMPLE_CONDITION_PATTERN = Pattern.compile(
            "^[A-Za-z0-9_\"'().+-]+\\s*(?:=|<>|<=|>=|<|>)\\s*[A-Za-z0-9_\"'().+-]+(?:\\s+(?:AND|OR)\\s+[A-Za-z0-9_\"'().+-]+\\s*(?:=|<>|<=|>=|<|>)\\s*[A-Za-z0-9_\"'().+-]+)*$",
            Pattern.CASE_INSENSITIVE
    );

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
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            final var matcher = IF_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                final String condition = matcher.group(2).trim();
                if (isLikelyCondition(condition)) {
                    return Optional.of("if (" + normalizeCondition(condition) + ") {");
                }
            }
            return Optional.empty();
        });
    }

    private boolean isLikelyCondition(String condition) {
        return SIMPLE_CONDITION_PATTERN.matcher(condition).matches();
    }

    private String normalizeCondition(String condition) {
        final StringBuilder result = new StringBuilder(condition.length());
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        for (int i = 0; i < condition.length(); i++) {
            final char current = condition.charAt(i);
            if (current == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                result.append(current);
                continue;
            }
            if (current == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                result.append(current);
                continue;
            }
            if (!inSingleQuotes && !inDoubleQuotes) {
                result.append(Character.toLowerCase(current));
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }
}
