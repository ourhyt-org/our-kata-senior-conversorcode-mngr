package art.ourhyt.legacy2modern.migration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CobolMoveRule implements Rule {
    private static final Pattern PATTERN = Pattern.compile("^\\s*MOVE\\s+(.+?)\\s+TO\\s+([A-Za-z0-9_-]+)\\s*$", Pattern.CASE_INSENSITIVE);

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
        final List<String> updated = new ArrayList<>(lines.size());
        final List<Integer> lineNumbers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                final String source = matcher.group(1).trim();
                final String destination = matcher.group(2).trim();
                updated.add(renderAssignment(source, destination, context.targetLanguage()));
                lineNumbers.add(i + 1);
            } else {
                updated.add(line);
            }
        }
        return new RuleResult(updated, lineNumbers.size(), lineNumbers);
    }

    private String renderAssignment(String source, String destination, TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA, NODE, GO -> destination + " = " + source + ";";
            case PYTHON -> destination + " = " + source;
        };
    }
}
