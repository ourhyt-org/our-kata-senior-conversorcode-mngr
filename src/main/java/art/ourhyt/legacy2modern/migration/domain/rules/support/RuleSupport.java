package art.ourhyt.legacy2modern.migration.domain.rules.support;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RuleSupport {
    private RuleSupport() {
    }

    public static RuleResult mapLines(List<String> lines, MigrationContext ctx, LineTransformer transformer) {
        final List<String> updatedLines = new ArrayList<>(lines.size());
        final List<Integer> lineNumbers = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final int lineNumber = i + 1;
            final Optional<String> transformed = transformer.transform(line, ctx, lineNumber);
            if (transformed.isPresent()) {
                updatedLines.add(transformed.get());
                lineNumbers.add(lineNumber);
            } else {
                updatedLines.add(line);
            }
        }

        return new RuleResult(updatedLines, lineNumbers.size(), lineNumbers);
    }

    @FunctionalInterface
    public interface LineTransformer {
        Optional<String> transform(String line, MigrationContext ctx, int lineNumber);
    }
}
