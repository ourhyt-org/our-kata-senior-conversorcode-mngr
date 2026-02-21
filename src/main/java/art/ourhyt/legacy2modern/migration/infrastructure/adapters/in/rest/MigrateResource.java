package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.migration.application.dto.MigrateRequestModel;
import art.ourhyt.legacy2modern.migration.application.dto.MigrateResponseModel;
import art.ourhyt.legacy2modern.migration.domain.ports.in.MigrateLegacyCodeInputPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
public class MigrateResource {
    private static final Logger LOG = Logger.getLogger(MigrateResource.class);

    private final MigrateLegacyCodeInputPort migrateLegacyCodeInputPort;
    private final MigrateHttpMapper mapper;

    @Inject
    public MigrateResource(MigrateLegacyCodeInputPort migrateLegacyCodeInputPort, MigrateHttpMapper mapper) {
        this.migrateLegacyCodeInputPort = migrateLegacyCodeInputPort;
        this.mapper = mapper;
    }

    @POST
    public MigrateHttpResponse migrate(@Valid MigrateHttpRequest request) {
        final String requestId = UUID.randomUUID().toString();
        final MigrateRequestModel useCaseRequest = mapper.toApplication(request);
        final MigrateResponseModel response = migrateLegacyCodeInputPort.execute(useCaseRequest);

        final int payloadSize = useCaseRequest.code() == null ? 0 : useCaseRequest.code().getBytes(StandardCharsets.UTF_8).length;
        final int rulesCount = response.report().appliedRules().size();
        final int warningCount = response.report().warnings().size();
        LOG.infov("requestId={0} sourceLang={1} targetLang={2} payloadBytes={3} appliedRules={4} warnings={5}", requestId, useCaseRequest.sourceLanguage(), useCaseRequest.targetLanguage(), payloadSize, rulesCount, warningCount);

        return mapper.fromApplication(response);
    }
}
