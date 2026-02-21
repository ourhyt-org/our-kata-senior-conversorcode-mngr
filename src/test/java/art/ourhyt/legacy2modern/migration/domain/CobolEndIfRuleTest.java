package art.ourhyt.legacy2modern.migration.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolEndIfRuleTest {
    @Test
    void transformsEndIf() {
        final CobolEndIfRule rule = new CobolEndIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("END-IF"), context);

        assertEquals("}", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
