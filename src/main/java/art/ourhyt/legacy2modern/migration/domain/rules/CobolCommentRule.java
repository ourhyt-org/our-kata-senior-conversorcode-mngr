package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.LegacyLineNormalizer;
import art.ourhyt.legacy2modern.migration.domain.rules.support.RuleSupport;

import java.util.List;
import java.util.Optional;

public final class CobolCommentRule implements Rule {
    @Override
    public String id() {
        return "RULE_COMMENT";
    }

    @Override
    public String name() {
        return "COBOL comments -> //";
    }

    @Override
    public String description() {
        return "Normalizes COBOL comments to double-slash comments";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.COBOL) {
            return new RuleResult(lines, 0, List.of());
        }
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            final String normalized = LegacyLineNormalizer.normalizeGeneric(line);
            if (normalized.startsWith("*>") || normalized.startsWith("*")) {
                final String body = normalized.startsWith("*>") ? normalized.substring(2).trim() : normalized.substring(1).trim();
                return Optional.of("// " + body);
            }
            return Optional.empty();
        });
    }
}
