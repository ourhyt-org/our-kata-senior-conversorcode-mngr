package art.ourhyt.legacy2modern.migration.infrastructure.config;

import art.ourhyt.legacy2modern.migration.application.PayloadPolicyPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PayloadPolicyConfigAdapter implements PayloadPolicyPort {
    @ConfigProperty(name = "migration.payload.max-bytes", defaultValue = "204800")
    int maxPayloadBytes;

    @ConfigProperty(name = "migration.payload.max-lines", defaultValue = "5000")
    int maxLines;

    @Override
    public int maxPayloadBytes() {
        return maxPayloadBytes;
    }

    @Override
    public int maxLines() {
        return maxLines;
    }
}
