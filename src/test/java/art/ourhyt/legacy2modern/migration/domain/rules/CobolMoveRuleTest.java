package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolMoveRuleTest {
    @Test
    void transformsMove() {
        final CobolMoveRule rule = new CobolMoveRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.PYTHON, null);

        final RuleResult result = rule.apply(List.of("MOVE A TO B"), context);

        assertEquals("B = A", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
