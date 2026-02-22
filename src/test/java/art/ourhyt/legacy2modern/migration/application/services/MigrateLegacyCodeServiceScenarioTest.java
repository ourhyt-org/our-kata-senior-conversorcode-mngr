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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrateLegacyCodeServiceScenarioTest {
    @Test
    void migratesKnownDottedCobolStatementsWithoutUnknownWarnings() {
        final var engine = new RuleBasedMigrationEngineAdapter(new InMemoryRuleCatalogAdapter());
        final var service = new MigrateLegacyCodeService(engine, new FixedPayloadPolicy(204800, 5000));

        final String code = String.join("\n",
            "IF A > B THEN.",
            "DISPLAY \"HI\".",
            "ELSE.",
            "DISPLAY \"BYE\".",
            "END-IF.",
            "STOP RUN.",
            "MOVE 10 TO AMOUNT."
        );

        final MigrateResponseModel response = service.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, code));

        final Set<String> ruleIds = response.report().appliedRules().stream().map(r -> r.id()).collect(Collectors.toSet());
        final Set<String> warningCodes = response.report().warnings().stream().map(w -> w.code()).collect(Collectors.toSet());

        assertTrue(ruleIds.contains("RULE_IF"));
        assertTrue(ruleIds.contains("RULE_DISPLAY"));
        assertTrue(ruleIds.contains("RULE_ELSE"));
        assertTrue(ruleIds.contains("RULE_END_IF"));
        assertTrue(ruleIds.contains("RULE_STOP_RUN"));
        assertTrue(ruleIds.contains("RULE_MOVE"));
        assertFalse(ruleIds.contains("RULE_UNKNOWN"));
        assertFalse(warningCodes.contains("WARN_UNKNOWN_SYNTAX"));
    }

    private record FixedPayloadPolicy(int maxPayloadBytes, int maxLines) implements PayloadPolicyPort {
    }
}
