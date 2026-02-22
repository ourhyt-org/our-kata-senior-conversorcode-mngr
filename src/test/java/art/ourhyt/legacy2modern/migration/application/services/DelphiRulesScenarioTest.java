package art.ourhyt.legacy2modern.migration.application.services;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import art.ourhyt.legacy2modern.migration.domain.ports.outputs.PayloadPolicyPort;
import art.ourhyt.legacy2modern.migration.infrastructure.adapters.output.engine.InMemoryRuleCatalogAdapter;
import art.ourhyt.legacy2modern.migration.infrastructure.adapters.output.engine.RuleBasedMigrationEngineAdapter;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelphiRulesScenarioTest {
    @Test
    void convertsDelphiBeginAssignmentWritelnEndForJava() {
        final var engine = new RuleBasedMigrationEngineAdapter(new InMemoryRuleCatalogAdapter());
        final var service = new MigrateLegacyCodeService(engine, new FixedPayloadPolicy(204800, 5000));

        final String input = String.join("\n",
            "begin",
            "  x := 1;",
            "  writeln('Hello');",
            "end."
        );

        final MigrateResponseModel response = service.execute(new MigrateRequestModel(SourceLanguage.DELPHI, TargetLanguage.JAVA, null, input));

        assertEquals("{\nx = 1;\nSystem.out.println(\"Hello\");\n}", response.outputCode());

        final Set<String> appliedRuleIds = response.report().appliedRules().stream().map(r -> r.id()).collect(Collectors.toSet());
        assertTrue(appliedRuleIds.contains("RULE_DELPHI_BEGIN_END"));
        assertTrue(appliedRuleIds.contains("RULE_DELPHI_ASSIGNMENT"));
        assertTrue(appliedRuleIds.contains("RULE_DELPHI_WRITELN"));

        assertFalse(appliedRuleIds.contains("RULE_DELPHI_UNKNOWN"));
        assertFalse(response.outputCode().contains("// TODO: UNMAPPED:"));
        assertFalse(response.report().warnings().stream().map(w -> w.code()).collect(Collectors.toSet()).contains("WARN_UNKNOWN_SYNTAX"));
    }

    private record FixedPayloadPolicy(int maxPayloadBytes, int maxLines) implements PayloadPolicyPort {
    }
}
