package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CobolStopRunRuleTest {
    @Test
    void transformsStopRun() {
        final CobolStopRunRule rule = new CobolStopRunRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("STOP RUN"), context);

        assertEquals("System.exit(0);", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void transformsStopRunWithTrailingDot() {
        final CobolStopRunRule rule = new CobolStopRunRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.COBOL, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("STOP RUN."), context);

        assertEquals("System.exit(0);", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
