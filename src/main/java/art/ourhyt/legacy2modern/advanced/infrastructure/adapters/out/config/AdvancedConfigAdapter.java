package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.config;

import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class AdvancedConfigAdapter implements AdvancedConfigPort {
    @ConfigProperty(name = "advanced.usage-table")
    String usageTable;

    @ConfigProperty(name = "advanced.default-daily-limit")
    int defaultDailyLimit;

    @ConfigProperty(name = "advanced.supabase-jwks-url", defaultValue = "")
    String supabaseJwksUrl;

    @ConfigProperty(name = "advanced.supabase-issuer", defaultValue = "")
    String supabaseIssuer;

    @ConfigProperty(name = "advanced.supabase-audience")
    Optional<String> supabaseAudience;

    @Override
    public String usageTable() {
        return usageTable;
    }

    @Override
    public int defaultDailyLimit() {
        return defaultDailyLimit;
    }

    @Override
    public String supabaseJwksUrl() {
        return supabaseJwksUrl;
    }

    @Override
    public String supabaseIssuer() {
        return supabaseIssuer;
    }

    @Override
    public String supabaseAudience() {
        return supabaseAudience.orElse("");
    }
}
