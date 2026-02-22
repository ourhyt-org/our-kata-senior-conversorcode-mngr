package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;

import java.util.ArrayList;
import java.util.List;

abstract class SourceSpecificRule implements Rule {
    @Override
    public final RuleResult apply(List<String> lines, MigrationContext context) {
        if (context.sourceLanguage() != sourceLanguage()) {
            return new RuleResult(lines, 0, List.of());
        }
        final List<String> updatedLines = new ArrayList<>(lines.size());
        final List<Integer> lineNumbers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final int lineNumber = i + 1;
            final LineTransform transform = transformLine(lines.get(i), context, lineNumber);
            updatedLines.add(transform.line());
            if (transform.matched()) {
                lineNumbers.add(lineNumber);
            }
        }
        return new RuleResult(updatedLines, lineNumbers.size(), lineNumbers);
    }

    protected abstract SourceLanguage sourceLanguage();

    protected abstract LineTransform transformLine(String line, MigrationContext context, int lineNumber);

    protected LineTransform keep(String line) {
        return new LineTransform(line, false);
    }

    protected LineTransform match(String line) {
        return new LineTransform(line, true);
    }

    protected record LineTransform(String line, boolean matched) {
    }
}
