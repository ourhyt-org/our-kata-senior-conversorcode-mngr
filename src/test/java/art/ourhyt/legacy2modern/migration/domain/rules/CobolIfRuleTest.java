package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolIfRuleTest {
    @Test
    void transformsIfThen() {
        final CobolIfRule rule = new CobolIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("IF A = B THEN"), context);

        assertEquals("if (a = b) {", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
        assertEquals(List.of(1), result.lineNumbers());
    }

    @Test
    void transformsIfThenWithTrailingDot() {
        final CobolIfRule rule = new CobolIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("IF A = B THEN."), context);

        assertEquals("if (a = b) {", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
        assertEquals(List.of(1), result.lineNumbers());
    }

    @Test
    void transformsIfWithoutThenWhenConditionIsValid() {
        final CobolIfRule rule = new CobolIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("IF AMOUNT > 0"), context);

        assertEquals("if (amount > 0) {", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
        assertEquals(List.of(1), result.lineNumbers());
    }

    @Test
    void doesNotTransformIfWithoutThenWhenConditionIsInvalid() {
        final CobolIfRule rule = new CobolIfRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("IF AMOUNT > 0 ttt"), context);

        assertEquals("IF AMOUNT > 0 ttt", result.updatedLines().getFirst());
        assertEquals(0, result.matchesCount());
        assertEquals(List.of(), result.lineNumbers());
    }
}
