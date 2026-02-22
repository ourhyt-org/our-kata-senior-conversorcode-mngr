package art.ourhyt.legacy2modern.migration.domain.rules.support;

public final class DelphiLiteralNormalizer {
    private DelphiLiteralNormalizer() {
    }

    public static String normalizeDelphiLiteralToTarget(String rawExpr) {
        if (rawExpr == null) {
            return "";
        }

        final String trimmed = rawExpr.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            final String unquoted = trimmed.substring(1, trimmed.length() - 1);
            final String unescaped = unquoted.replace("''", "'");
            return '"' + unescaped + '"';
        }

        return trimmed;
    }
}
