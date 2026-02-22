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

public final class CobolEndIfRule implements Rule {
    private static final Pattern END_IF_PATTERN = Patterns.cobolStmt("END-IF");

    @Override
    public String id()  {
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
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            if (END_IF_PATTERN.matcher(normalized).matches()) {
                return Optional.of("}");
            }
            return Optional.empty();
        });
    }
}
