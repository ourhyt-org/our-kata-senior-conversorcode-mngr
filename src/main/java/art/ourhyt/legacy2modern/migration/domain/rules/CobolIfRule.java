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
            Patterns.cobolStmt("IF\\s+(.+?)\\s+THEN\\b");

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
                return Optional.of("if (" + matcher.group(2).trim() + ") {");
            }
            return Optional.empty();
        });
    }
}
