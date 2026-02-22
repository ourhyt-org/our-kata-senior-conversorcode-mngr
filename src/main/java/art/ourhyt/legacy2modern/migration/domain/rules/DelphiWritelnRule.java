package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import art.ourhyt.legacy2modern.migration.domain.rules.support.DelphiLiteralNormalizer;

import java.util.regex.Pattern;

public final class DelphiWritelnRule extends SourceSpecificRule {
    private static final Pattern WRITELN_PATTERN = Pattern.compile("^\\s*writeln\\s*\\((.+?)\\)\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Override
    public String id() {
        return "RULE_DELPHI_WRITELN";
    }

    @Override
    public String name() {
        return "DELPHI writeln -> print";
    }

    @Override
    public String description() {
        return "Converts Delphi writeln calls to target print functions";
    }

    @Override
    protected SourceLanguage sourceLanguage() {
        return SourceLanguage.DELPHI;
    }

    @Override
    protected LineTransform transformLine(String line, MigrationContext context, int lineNumber) {
        final var matcher = WRITELN_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return keep(line);
        }

        final String payload = normalizePayload(matcher.group(1));
        return match(renderPrint(payload, context.targetLanguage()));
    }

    private String normalizePayload(String rawPayload) {
        final String normalized = DelphiLiteralNormalizer.normalizeDelphiLiteralToTarget(rawPayload);
        if (isDoubleQuoted(normalized)) {
            return normalized;
        }
        if (shouldQuoteAsPlainText(normalized)) {
            return "\"" + escapeForDoubleQuotes(normalized) + "\"";
        }
        return normalized;
    }

    private boolean shouldQuoteAsPlainText(String payload) {
        if (payload.isBlank()) {
            return false;
        }
        if (IDENTIFIER_PATTERN.matcher(payload).matches()) {
            return Character.isUpperCase(payload.charAt(0));
        }
        return payload.contains(" ") && !looksLikeExpression(payload);
    }

    private boolean looksLikeExpression(String payload) {
        return payload.matches(".*[+\\-*/%<>=!&|^()\\[\\]{}].*");
    }

    private boolean isDoubleQuoted(String payload) {
        return payload.length() >= 2 && payload.startsWith("\"") && payload.endsWith("\"");
    }

    private String escapeForDoubleQuotes(String payload) {
        return payload.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String renderPrint(String payload, TargetLanguage targetLanguage) {
        return switch (targetLanguage) {
            case JAVA -> "System.out.println(" + payload + ");";
            case NODE -> "console.log(" + payload + ");";
            case PYTHON -> "print(" + payload + ")";
            case GO -> "fmt.Println(" + payload + ")";
        };
    }
}
