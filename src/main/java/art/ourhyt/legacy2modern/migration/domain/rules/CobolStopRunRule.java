package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.LegacyLineNormalizer;
import art.ourhyt.legacy2modern.migration.domain.rules.support.Patterns;
import art.ourhyt.legacy2modern.migration.domain.rules.support.RuleSupport;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CobolStopRunRule implements Rule {
    private static final Pattern STOP_RUN_PATTERN = Patterns.cobolStmt("STOP\\s+RUN");

    @Override
    public String id() {
        return "RULE_STOP_RUN";
    }

    @Override
    public String name() {
        return "COBOL STOP RUN -> return";
    }

    @Override
    public String description() {
        return "Transforms COBOL STOP RUN into target-specific exit";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.COBOL) {
            return new RuleResult(lines, 0, List.of());
        }
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            if (STOP_RUN_PATTERN.matcher(normalized).matches()) {
                return Optional.of(renderStop(ctx.targetLanguage()));
            }
            return Optional.empty();
        });
    }

    private String renderStop(TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA -> "System.exit(0);";
            case NODE, PYTHON, GO -> "return;";
        };
    }
}
