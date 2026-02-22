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

public final class CobolElseRule implements Rule {
    private static final Pattern ELSE_PATTERN = Patterns.cobolStmt("ELSE");

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
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            if (ELSE_PATTERN.matcher(normalized).matches()) {
                return Optional.of("} else {");
            }
            return Optional.empty();
        });
    }
}
