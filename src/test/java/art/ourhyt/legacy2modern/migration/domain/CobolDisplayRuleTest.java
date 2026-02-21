package art.ourhyt.legacy2modern.migration.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolDisplayRuleTest {
    @Test
    void transformsDisplay() {
        final CobolDisplayRule rule = new CobolDisplayRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.NODE, null);

        final RuleResult result = rule.apply(List.of("DISPLAY 'HELLO'"), context);

        assertEquals("console.log(\"HELLO\");", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
