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

public final class CobolDisplayRule implements Rule {
    private static final Pattern DISPLAY_PATTERN = Patterns.cobolStmt("DISPLAY\\s+(.+?)");

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
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            final var matcher = DISPLAY_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                final String payload = normalizeLiteral(matcher.group(2));
                return Optional.of(renderDisplay(payload, ctx.targetLanguage()));
            }
            return Optional.empty();
        });
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
            case JAVA -> "logger.info(" + payload + ");";
            case NODE -> "console.log(" + payload + ");";
            case PYTHON -> "print(" + payload + ")";
            case GO -> "fmt.Println(" + payload + ")";
        };
    }
}
