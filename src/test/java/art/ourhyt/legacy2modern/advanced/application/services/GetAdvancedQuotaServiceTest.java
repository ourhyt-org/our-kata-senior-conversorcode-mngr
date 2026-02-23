package art.ourhyt.legacy2modern.advanced.application.services;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;
import art.ourhyt.legacy2modern.advanced.domain.model.AuthenticatedUser;
import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedQuotaRepositoryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAdvancedQuotaServiceTest {
    @Test
    void returnsDefaultWhenNoUsage() {
        final GetAdvancedQuotaService service = new GetAdvancedQuotaService(
            header -> new AuthenticatedUser("user-1", null),
            new AdvancedQuotaRepositoryPort() {
                @Override
                public ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit) {
                    return new ConsumeQuotaResult(true, new QuotaStatus(10, 1, 9, "2026-02-23T00:00:00Z"));
                }

                @Override
                public QuotaStatus getToday(String userId, int defaultDailyLimit) {
                    return new QuotaStatus(defaultDailyLimit, 0, defaultDailyLimit, "2026-02-23T00:00:00Z");
                }
            },
            new FixedConfig()
        );

        final AdvancedQuotaResponseModel response = service.execute("Bearer ok");
        assertEquals(10, response.quota().limit());
        assertEquals(0, response.quota().used());
    }

    @Test
    void returnsExistingUsage() {
        final GetAdvancedQuotaService service = new GetAdvancedQuotaService(
            header -> new AuthenticatedUser("user-1", null),
            new AdvancedQuotaRepositoryPort() {
                @Override
                public ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit) {
                    return new ConsumeQuotaResult(true, new QuotaStatus(10, 1, 9, "2026-02-23T00:00:00Z"));
                }

                @Override
                public QuotaStatus getToday(String userId, int defaultDailyLimit) {
                    return new QuotaStatus(10, 4, 6, "2026-02-23T00:00:00Z");
                }
            },
            new FixedConfig()
        );

        final AdvancedQuotaResponseModel response = service.execute("Bearer ok");
        assertEquals(4, response.quota().used());
        assertEquals(6, response.quota().remaining());
    }

    private record FixedConfig() implements AdvancedConfigPort {
        @Override
        public String usageTable() {
            return "kata-advanced-usage-qa";
        }

        @Override
        public int defaultDailyLimit() {
            return 10;
        }

        @Override
        public String supabaseJwksUrl() {
            return "https://example.supabase.co/auth/v1/.well-known/jwks.json";
        }

        @Override
        public String supabaseIssuer() {
            return "https://example.supabase.co/auth/v1";
        }

        @Override
        public String supabaseAudience() {
            return "";
        }
    }
}
