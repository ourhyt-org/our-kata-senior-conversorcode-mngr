package art.ourhyt.legacy2modern.migration.domain.rules.support;

import java.util.regex.Pattern;

public final class Patterns {
    private Patterns() {
    }

    public static Pattern cobolStmt(String bodyRegex) {
        return Pattern.compile("^\\s*(" + bodyRegex + ")\\s*\\.?\\s*$", Pattern.CASE_INSENSITIVE);
    }
}
