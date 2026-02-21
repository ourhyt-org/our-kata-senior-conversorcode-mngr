package art.ourhyt.legacy2modern.migration.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolCommentRuleTest {
    @Test
    void normalizesComment() {
        final CobolCommentRule rule = new CobolCommentRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("*> test"), context);

        assertEquals("// test", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
