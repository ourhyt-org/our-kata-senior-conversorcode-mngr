package art.ourhyt.legacy2modern.migration.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolIfRuleTest {
    @Test
    void transformsIfThen() {
        final CobolIfRule rule = new CobolIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("IF A = B THEN"), context);

        assertEquals("if (A = B) {", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
        assertEquals(List.of(1), result.lineNumbers());
    }
}
