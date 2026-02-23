package art.ourhyt.legacy2modern.advanced.domain.ports.out;

public interface AdvancedConfigPort {
    String usageTable();

    int defaultDailyLimit();

    String supabaseJwksUrl();

    String supabaseIssuer();

    String supabaseAudience();
}
