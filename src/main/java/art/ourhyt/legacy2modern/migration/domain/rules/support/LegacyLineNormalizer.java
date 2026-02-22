package art.ourhyt.legacy2modern.migration.domain.rules.support;

public final class LegacyLineNormalizer {
    private LegacyLineNormalizer() {
    }

    public static String normalizeCobolStatement(String line) {
        if (line == null) {
            return "";
        }
        final String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        final int lastNonSpaceIndex = lastNonSpaceIndex(trimmed);
        if (lastNonSpaceIndex < 0 || trimmed.charAt(lastNonSpaceIndex) != '.') {
            return trimmed;
        }

        if (isInsideQuote(trimmed, lastNonSpaceIndex)) {
            return trimmed;
        }

        return trimmed.substring(0, lastNonSpaceIndex).stripTrailing();
    }

    public static String normalizeGeneric(String line) {
        if (line == null) {
            return "";
        }
        return line.trim();
    }

    private static int lastNonSpaceIndex(String value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isInsideQuote(String value, int index) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i <= index; i++) {
            final char c = value.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
        }
        return inSingleQuote || inDoubleQuote;
    }
}
