package art.ourhyt;

import art.ourhyt.legacy2modern.migration.application.MigrateLegacyCodeUseCase;
import art.ourhyt.legacy2modern.migration.application.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.infrastructure.rest.MigrateHttpMapper;
import art.ourhyt.legacy2modern.migration.infrastructure.rest.MigrateHttpRequest;
import art.ourhyt.legacy2modern.migration.infrastructure.rest.MigrateHttpResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Path("/migrate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GreetingResource {
    private static final Logger LOG = Logger.getLogger(GreetingResource.class);

    private final MigrateLegacyCodeUseCase useCase;
    private final MigrateHttpMapper mapper;

    public GreetingResource(MigrateLegacyCodeUseCase useCase, MigrateHttpMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @POST
    public MigrateHttpResponse migrate(MigrateHttpRequest request) {
        final String requestId = UUID.randomUUID().toString();
        final MigrateRequestModel useCaseRequest = mapper.toApplication(request);
        final MigrateResponseModel response = useCase.execute(useCaseRequest);

        final int payloadSize = useCaseRequest.code() == null ? 0 : useCaseRequest.code().getBytes(StandardCharsets.UTF_8).length;
        final int rulesCount = response.report().appliedRules().size();
        final int warningCount = response.report().warnings().size();
        LOG.infov("requestId={0} sourceLang={1} targetLang={2} payloadBytes={3} appliedRules={4} warnings={5}", requestId, useCaseRequest.sourceLanguage(), useCaseRequest.targetLanguage(), payloadSize, rulesCount, warningCount);

        return mapper.fromApplication(response);
    }
}
