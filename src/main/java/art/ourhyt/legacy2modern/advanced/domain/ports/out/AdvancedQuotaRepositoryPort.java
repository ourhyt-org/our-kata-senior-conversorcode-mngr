package art.ourhyt.legacy2modern.advanced.domain.ports.out;

import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;

public interface AdvancedQuotaRepositoryPort {
    ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit);

    QuotaStatus getToday(String userId, int defaultDailyLimit);
}
