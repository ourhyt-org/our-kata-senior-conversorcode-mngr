package art.ourhyt.legacy2modern.migration.domain.rules;

import art.ourhyt.legacy2modern.migration.domain.model.MigrationContext;
import art.ourhyt.legacy2modern.migration.domain.model.RuleResult;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DelphiWritelnRuleTest {
    @Test
    void convertsSimpleSingleQuotedLiteral() {
        final DelphiWritelnRule rule = new DelphiWritelnRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("writeln('Hello');"), context);

        assertEquals("System.out.println(\"Hello\");", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void convertsEscapedSingleQuotedLiteral() {
        final DelphiWritelnRule rule = new DelphiWritelnRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.NODE, null);

        final RuleResult result = rule.apply(List.of("writeln('It''s ok');"), context);

        assertEquals("console.log(\"It's ok\");", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void keepsNonStringExpressionAsIs() {
        final DelphiWritelnRule rule = new DelphiWritelnRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("writeln(total);"), context);

        assertEquals("System.out.println(total);", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void quotesUppercaseIdentifierAsPlainText() {
        final DelphiWritelnRule rule = new DelphiWritelnRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.JAVA, null);

        final RuleResult result = rule.apply(List.of("writeln(Hello);"), context);

        assertEquals("System.out.println(\"Hello\");", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }

    @Test
    void quotesSpacedPayloadAsPlainText() {
        final DelphiWritelnRule rule = new DelphiWritelnRule();
        final MigrationContext context = new MigrationContext(SourceLanguage.DELPHI, TargetLanguage.NODE, null);

        final RuleResult result = rule.apply(List.of("writeln(Its ok);"), context);

        assertEquals("console.log(\"Its ok\");", result.updatedLines().getFirst());
        assertEquals(1, result.matchesCount());
    }
}
