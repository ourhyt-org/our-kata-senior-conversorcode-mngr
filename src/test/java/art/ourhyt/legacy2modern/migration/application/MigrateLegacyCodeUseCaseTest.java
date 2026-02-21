package art.ourhyt.legacy2modern.migration.application;

import art.ourhyt.legacy2modern.migration.domain.CobolCommentRule;
import art.ourhyt.legacy2modern.migration.domain.CobolDisplayRule;
import art.ourhyt.legacy2modern.migration.domain.CobolElseRule;
import art.ourhyt.legacy2modern.migration.domain.CobolEndIfRule;
import art.ourhyt.legacy2modern.migration.domain.CobolIfRule;
import art.ourhyt.legacy2modern.migration.domain.CobolMoveRule;
import art.ourhyt.legacy2modern.migration.domain.CobolStopRunRule;
import art.ourhyt.legacy2modern.migration.domain.CobolUnknownLineRule;
import art.ourhyt.legacy2modern.migration.domain.Rule;
import art.ourhyt.legacy2modern.migration.domain.SourceLanguage;
import art.ourhyt.legacy2modern.migration.domain.TargetLanguage;
import art.ourhyt.legacy2modern.migration.infrastructure.engine.RuleBasedMigrationEngineAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrateLegacyCodeUseCaseTest {
    @Test
    void rejectsEmptyCode() {
        final MigrateLegacyCodeUseCase useCase = new MigrateLegacyCodeUseCase(new RuleBasedMigrationEngineAdapter(language -> List.of()), new FixedPayloadPolicy(204800, 5000));

        assertThrows(ValidationException.class, () -> useCase.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, " ")));
    }

    @Test
    void rejectsLargePayload() {
        final MigrateLegacyCodeUseCase useCase = new MigrateLegacyCodeUseCase(new RuleBasedMigrationEngineAdapter(language -> List.of()), new FixedPayloadPolicy(3, 5000));

        assertThrows(PayloadTooLargeException.class, () -> useCase.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, "1234")));
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
        final MigrateLegacyCodeUseCase useCase = new MigrateLegacyCodeUseCase(new RuleBasedMigrationEngineAdapter(catalogPort), new FixedPayloadPolicy(204800, 5000));
        final String code = "IF A = B THEN\nDISPLAY 'ok'\nELSE\nACCEPT X\nEND-IF";

        final MigrateResponseModel response = useCase.execute(new MigrateRequestModel(SourceLanguage.COBOL, TargetLanguage.JAVA, null, code));

        assertFalse(response.report().appliedRules().isEmpty());
        assertFalse(response.report().warnings().isEmpty());
    }

    private record FixedPayloadPolicy(int maxPayloadBytes, int maxLines) implements PayloadPolicyPort {
    }
}
