package art.ourhyt.legacy2modern.migration.application.services;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.application.exceptions.PayloadTooLargeException;
import art.ourhyt.legacy2modern.migration.application.exceptions.ValidationException;
import art.ourhyt.legacy2modern.migration.domain.model.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.model.TargetLanguage;
import art.ourhyt.legacy2modern.migration.domain.ports.out.PayloadPolicyPort;
import art.ourhyt.legacy2modern.migration.domain.ports.out.RuleCatalogPort;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolCommentRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolDisplayRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolElseRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolEndIfRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolIfRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolMoveRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolStopRunRule;
import art.ourhyt.legacy2modern.migration.domain.rules.CobolUnknownLineRule;
import art.ourhyt.legacy2modern.migration.infrastructure.adapters.out.engine.RuleBasedMigrationEngineAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrateLegacyCodeServiceTest {
    @Test
    void rejectsEmptyCode() {
        final MigrateLegacyCodeService service = new MigrateLegacyCodeService(new RuleBasedMigrationEngineAdapter(language -> List.of()), new FixedPayloadPolicy(204800, 5000));

        assertThrows(ValidationException.class, () -> service.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, " ")));
    }

    @Test
    void rejectsLargePayload() {
        final MigrateLegacyCodeService service = new MigrateLegacyCodeService(new RuleBasedMigrationEngineAdapter(language -> List.of()), new FixedPayloadPolicy(3, 5000));

        assertThrows(PayloadTooLargeException.class, () -> service.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, "1234")));
    }

    @Test
    void returnsReportWithAppliedRulesAndWarnings() {
        final RuleCatalogPort catalogPort = sourceLanguage -> List.of(
            new CobolIfRule(),
            new CobolElseRule(),
            new CobolEndIfRule(),
            new CobolDisplayRule(),
            new CobolMoveRule(),
            new CobolStopRunRule(),
            new CobolCommentRule(),
            new CobolUnknownLineRule()
        );
        final MigrateLegacyCodeService service = new MigrateLegacyCodeService(new RuleBasedMigrationEngineAdapter(catalogPort), new FixedPayloadPolicy(204800, 5000));
        final String code = "IF A = B THEN\nDISPLAY 'ok'\nELSE\nACCEPT X\nEND-IF";

        final MigrateResponseModel response = service.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, code));

        assertFalse(response.report().appliedRules().isEmpty());
        assertFalse(response.report().warnings().isEmpty());
    }

    private record FixedPayloadPolicy(int maxPayloadBytes, int maxLines) implements PayloadPolicyPort {
    }
}
