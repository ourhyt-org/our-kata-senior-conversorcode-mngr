package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.DelphiLiteralNormalizer;

import java.util.regex.Pattern;

public final class DelphiAssignmentRule extends SourceSpecificRule {
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*:=\\s*(.+?)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return "RULE_DELPHI_ASSIGNMENT";
    }

    @Override
    public String name() {
        return "DELPHI := -> assignment";
    }

    @Override
    public String description() {
        return "Converts Delphi assignment operator to target assignment syntax";
    }

    @Override
    protected SourceLanguage sourceLanguage() {
        return SourceLanguage.DELPHI;
    }

    @Override
    protected LineTransform transformLine(String line, MigrationContext context, int lineNumber) {
        final var matcher = ASSIGNMENT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return keep(line);
        }

        final String identifier = matcher.group(1).trim();
        final String expression = normalizeExpression(matcher.group(2));
        return match(renderAssignment(identifier, expression, context.targetLanguage()));
    }

    private String normalizeExpression(String rawExpression) {
        final String normalized = DelphiLiteralNormalizer.normalizeDelphiLiteralToTarget(rawExpression);
        if (isDoubleQuoted(normalized)) {
            return normalized;
        }
        if (shouldQuoteAsPlainText(normalized)) {
            return "\"" + escapeForDoubleQuotes(normalized) + "\"";
        }
        return normalized;
    }

    private boolean shouldQuoteAsPlainText(String expression) {
        if (expression.isBlank()) {
            return false;
        }
        return expression.contains(" ") && !looksLikeExpression(expression);
    }

    private boolean looksLikeExpression(String expression) {
        return expression.matches(".*[+\\-*/%<>=!&|^()\\[\\]{}].*");
    }

    private boolean isDoubleQuoted(String expression) {
        return expression.length() >= 2 && expression.startsWith("\"") && expression.endsWith("\"");
    }

    private String escapeForDoubleQuotes(String expression) {
        return expression.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String renderAssignment(String identifier, String expression, TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA, NODE, GO -> identifier + " = " + expression + ";";
            case PYTHON -> identifier + " = " + expression;
        };
    }
}
