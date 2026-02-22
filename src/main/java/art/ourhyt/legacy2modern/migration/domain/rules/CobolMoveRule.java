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

public final class CobolMoveRule implements Rule {
    private static final Pattern MOVE_PATTERN = Patterns.cobolStmt("MOVE\\s+(.+?)\\s+TO\\s+([A-Za-z0-9_-]+)");

    @Override
    public String id() {
        return "RULE_MOVE";
    }

    @Override
    public String name() {
        return "COBOL MOVE -> assignment";
    }

    @Override
    public String description() {
        return "Transforms COBOL MOVE A TO B into assignment";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.COBOL) {
            return new RuleResult(lines, 0, List.of());
        }
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeCobolStatement(line);
            final var matcher = MOVE_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                final String source = matcher.group(2).trim();
                final String destination = matcher.group(3).trim();
                return Optional.of(renderAssignment(source, destination, ctx.targetLanguage()));
            }
            return Optional.empty();
        });
    }

    private String renderAssignment(String source, String destination, TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA, NODE, GO -> destination + " = " + source + ";";
            case PYTHON -> destination + " = " + source;
        };
    }
}
