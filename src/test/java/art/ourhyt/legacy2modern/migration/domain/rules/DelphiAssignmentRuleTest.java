package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelphiAssignmentRuleTest {
    @Test
    void convertsSimpleSingleQuotedLiteral() {
        final DelphiAssignmentRule rule = new DelphiAssignmentRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("name := 'Hello';"), context);

        assertEquals("name = \"Hello\";", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void convertsEscapedSingleQuotedLiteral() {
        final DelphiAssignmentRule rule = new DelphiAssignmentRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("name := 'It''s ok';"), context);

        assertEquals("name = \"It's ok\";", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void keepsNonStringExpressionAsIs() {
        final DelphiAssignmentRule rule = new DelphiAssignmentRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("total := 1 + 2;"), context);

        assertEquals("total = 1 + 2;", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void quotesSpacedUnquotedText() {
        final DelphiAssignmentRule rule = new DelphiAssignmentRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("name := Its ok;"), context);

        assertEquals("name = \"Its ok\";", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
