package art.ourhyt.legacy2modern.advanced.application.services;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.QuotaHttpModel;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.GetAdvancedQuotaInputPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedQuotaRepositoryPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.JwtVerifierPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GetAdvancedQuotaService implements GetAdvancedQuotaInputPort {
    private static final Logger LOG = Logger.getLogger(GetAdvancedQuotaService.class);
    private final JwtVerifierPort jwtVerifier;
    private final AdvancedQuotaRepositoryPort quotaRepository;
    private final AdvancedConfigPort config;

    @Inject
    public GetAdvancedQuotaService(JwtVerifierPort jwtVerifier, AdvancedQuotaRepositoryPort quotaRepository, AdvancedConfigPort config) {
        this.jwtVerifier = jwtVerifier;
        this.quotaRepository = quotaRepository;
        this.config = config;
    }

    @Override
    public AdvancedQuotaResponseModel execute(String authorizationHeader) {
        final String userId = jwtVerifier.verifyAuthorizationHeader(authorizationHeader).userId();
        LOG.infov("userId={0} step=advanced_quota_read_start", userId);
        final QuotaStatus quota = quotaRepository.getToday(userId, config.defaultDailyLimit());
        LOG.infov("userId={0} step=advanced_quota_read_done used={1} limit={2}", userId, quota.used(), quota.limit());
        return new AdvancedQuotaResponseModel(new QuotaHttpModel(quota.limit(), quota.used(), quota.remaining(), quota.resetAt()));
    }
}
