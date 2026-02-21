package art.ourhyt.legacy2modern.migration.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolUnknownLineRuleTest {
    @Test
    void wrapsUnknownLineAsComment() {
        final CobolUnknownLineRule rule = new CobolUnknownLineRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("ADD A TO B"), context);

        assertEquals("// TODO: UNMAPPED: ADD A TO B", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
