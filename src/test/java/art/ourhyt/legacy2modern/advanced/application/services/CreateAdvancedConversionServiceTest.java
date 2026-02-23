package art.ourhyt.legacy2modern.advanced.application.services;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionRequestModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;
import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedQuotaExceededException;
import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import art.ourhyt.legacy2modern.advanced.domain.model.AuthenticatedUser;
import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedQuotaRepositoryPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.JwtVerifierPort;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateAdvancedConversionServiceTest {
    @Test
    void rejectsInvalidToken() {
        final CreateAdvancedConversionService service = new CreateAdvancedConversionService(
            header -> {
                throw new AdvancedUnauthorizedException("UNAUTHORIZED", "Invalid or missing bearer token", java.util.List.of("bad token"));
            },
            new FixedQuotaRepository(new ConsumeQuotaResult(true, new QuotaStatus(10, 1, 9, "2026-02-23T00:00:00Z"))),
            new FixedConfig(),
            request -> new CreateConversionResponseModel("job-1", "PENDING", "/conversions/job-1")
        );

        assertThrows(
            AdvancedUnauthorizedException.class,
            () -> service.execute("Bearer bad", request())
        );
    }

    @Test
    void returnsQuotaExceededWhenLimitReached() {
        final CreateAdvancedConversionService service = new CreateAdvancedConversionService(
            header -> new AuthenticatedUser("user-1", "a@a.com"),
            new FixedQuotaRepository(new ConsumeQuotaResult(false, new QuotaStatus(10, 10, 0, "2026-02-23T00:00:00Z"))),
            new FixedConfig(),
            request -> new CreateConversionResponseModel("job-1", "PENDING", "/conversions/job-1")
        );

        final AdvancedQuotaExceededException exception = assertThrows(
            AdvancedQuotaExceededException.class,
            () -> service.execute("Bearer ok", request())
        );

        assertEquals("QUOTA_EXCEEDED", exception.code());
    }

    @Test
    void allowedReturnsPendingAndQuota() {
        final CreateAdvancedConversionService service = new CreateAdvancedConversionService(
            header -> new AuthenticatedUser("user-1", "a@a.com"),
            new FixedQuotaRepository(new ConsumeQuotaResult(true, new QuotaStatus(10, 3, 7, "2026-02-23T00:00:00Z"))),
            new FixedConfig(),
            request -> new CreateConversionResponseModel("job-99", "PENDING", "/conversions/job-99")
        );

        final AdvancedCreateConversionResponseModel response = service.execute("Bearer ok", request());

        assertEquals("job-99", response.jobId());
        assertEquals(10, response.quota().limit());
        assertEquals(3, response.quota().used());
        assertEquals(7, response.quota().remaining());
    }

    @Test
    void quotaConsumedEvenWhenEnqueueFails() {
        final TrackingQuotaRepository quotaRepository = new TrackingQuotaRepository();
        final CreateAdvancedConversionService service = new CreateAdvancedConversionService(
            header -> new AuthenticatedUser("user-1", null),
            quotaRepository,
            new FixedConfig(),
            request -> {
                throw new IllegalStateException("enqueue failed");
            }
        );

        assertThrows(IllegalStateException.class, () -> service.execute("Bearer ok", request()));
        assertEquals(1, quotaRepository.consumeCalls);
    }

    private AdvancedCreateConversionRequestModel request() {
        return new AdvancedCreateConversionRequestModel("cobol", "java", "21", "hexagonal", "DISPLAY 'HI'", Map.of());
    }

    private static final class TrackingQuotaRepository implements AdvancedQuotaRepositoryPort {
        int consumeCalls;

        @Override
        public ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit) {
            consumeCalls++;
            return new ConsumeQuotaResult(true, new QuotaStatus(10, 1, 9, "2026-02-23T00:00:00Z"));
        }

        @Override
        public QuotaStatus getToday(String userId, int defaultDailyLimit) {
            return new QuotaStatus(10, 1, 9, "2026-02-23T00:00:00Z");
        }
    }

    private static final class FixedQuotaRepository implements AdvancedQuotaRepositoryPort {
        private final ConsumeQuotaResult consumeResult;

        private FixedQuotaRepository(ConsumeQuotaResult consumeResult) {
            this.consumeResult = consumeResult;
        }

        @Override
        public ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit) {
            return consumeResult;
        }

        @Override
        public QuotaStatus getToday(String userId, int defaultDailyLimit) {
            return consumeResult.quotaStatus();
        }
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
