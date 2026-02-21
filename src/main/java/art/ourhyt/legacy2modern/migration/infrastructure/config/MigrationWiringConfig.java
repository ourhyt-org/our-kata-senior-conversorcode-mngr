package art.ourhyt.legacy2modern.migration.infrastructure.config;

import art.ourhyt.legacy2modern.migration.application.MigrateLegacyCodeUseCase;
import art.ourhyt.legacy2modern.migration.application.MigrationEnginePort;
import art.ourhyt.legacy2modern.migration.application.PayloadPolicyPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MigrationWiringConfig {
    @Produces
    @ApplicationScoped
    public MigrateLegacyCodeUseCase migrateLegacyCodeUseCase(MigrationEnginePort migrationEnginePort, PayloadPolicyPort payloadPolicyPort) {
        return new MigrateLegacyCodeUseCase(migrationEnginePort, payloadPolicyPort);
    }
}
