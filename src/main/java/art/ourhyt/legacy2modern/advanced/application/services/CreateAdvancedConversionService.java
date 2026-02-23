package art.ourhyt.legacy2modern.advanced.application.services;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionRequestModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.QuotaHttpModel;
import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedQuotaExceededException;
import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.CreateAdvancedConversionInputPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedQuotaRepositoryPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.JwtVerifierPort;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CreateAdvancedConversionService implements CreateAdvancedConversionInputPort {
    private final JwtVerifierPort jwtVerifier;
    private final AdvancedQuotaRepositoryPort quotaRepository;
    private final AdvancedConfigPort config;
    private final CreateConversionJobInputPort createConversionJobInputPort;

    @Inject
    public CreateAdvancedConversionService(JwtVerifierPort jwtVerifier, AdvancedQuotaRepositoryPort quotaRepository, AdvancedConfigPort config, CreateConversionJobInputPort createConversionJobInputPort) {
        this.jwtVerifier = jwtVerifier;
        this.quotaRepository = quotaRepository;
        this.config = config;
        this.createConversionJobInputPort = createConversionJobInputPort;
    }

    @Override
    public AdvancedCreateConversionResponseModel execute(String authorizationHeader, AdvancedCreateConversionRequestModel request) {
        final String userId = jwtVerifier.verifyAuthorizationHeader(authorizationHeader).userId();
        final ConsumeQuotaResult consumed = quotaRepository.consumeDaily(userId, config.defaultDailyLimit());
        if (!consumed.allowed()) {
            final QuotaStatus quota = consumed.quotaStatus();
            throw new AdvancedQuotaExceededException(
                "QUOTA_EXCEEDED",
                "Daily limit reached",
                List.of(
                    "limit=" + quota.limit(),
                    "used=" + quota.used(),
                    "resetAt=" + quota.resetAt()
                )
            );
        }

        final CreateConversionResponseModel baseResponse = createConversionJobInputPort.execute(
            new CreateConversionRequestModel(
                request.languageSelected(),
                request.languageTarget(),
                request.version(),
                request.typeArchitected(),
                request.code(),
                request.options() == null ? Map.of() : request.options()
            )
        );

        final QuotaStatus quotaStatus = consumed.quotaStatus();
        return new AdvancedCreateConversionResponseModel(
            baseResponse.jobId(),
            baseResponse.status(),
            baseResponse.pollUrl(),
            new QuotaHttpModel(quotaStatus.limit(), quotaStatus.used(), quotaStatus.remaining(), quotaStatus.resetAt())
        );
    }
}
