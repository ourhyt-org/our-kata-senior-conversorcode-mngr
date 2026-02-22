package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.LegacyLineNormalizer;
import art.ourhyt.legacy2modern.migration.domain.rules.support.RuleSupport;

import java.util.List;
import java.util.Optional;

public final class CobolUnknownLineRule implements Rule {
    @Override
    public String id() {
        return "RULE_UNKNOWN";
    }

    @Override
    public String name() {
        return "Unknown COBOL line -> comment";
    }

    @Override
    public String description() {
        return "Keeps unknown COBOL syntax as comments";
    }

    @Override
    public RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != SourceLanguage.COBOL) {
            return new RuleResult(lines, 0, List.of());
        }
        return RuleSupport.mapLines(lines, context, (line, ctx, lineNumber) -> {
            if (shouldWrapAsUnknown(line)) {
                return Optional.of("// TODO: UNMAPPED: " + LegacyLineNormalizer.normalizeGeneric(line));
            }
            return Optional.empty();
        });
    }

    private boolean shouldWrapAsUnknown(String line) {
        final String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (trimmed.startsWith("//") || trimmed.equals("}") || trimmed.equals("} else {") || trimmed.startsWith("if (")) {
            return false;
        }
        if (trimmed.contains("=") || trimmed.startsWith("System.out.println") || trimmed.startsWith("console.log") || trimmed.startsWith("print(") || trimmed.startsWith("fmt.Println") || trimmed.equals("System.exit(0);") || trimmed.equals("return;")) {
            return false;
        }
        return true;
    }
}
