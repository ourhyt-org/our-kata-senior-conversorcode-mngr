package art.ourhyt.legacy2modern.migration.domain.rules.support;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternsTest {
    @Test
    void cobolStmtMatchesWithAndWithoutTrailingDot() {
        final Pattern pattern = Patterns.cobolStmt("STOP\\s+RUN");

        assertTrue(pattern.matcher("STOP RUN").matches());
        assertTrue(pattern.matcher("STOP RUN.").matches());
    }
}
