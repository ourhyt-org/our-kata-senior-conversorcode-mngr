package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.CreateAdvancedConversionInputPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.GetAdvancedQuotaInputPort;
import art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest.CreateConversionHttpRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/advanced")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdvancedResource {
    private final CreateAdvancedConversionInputPort createAdvancedConversionInputPort;
    private final GetAdvancedQuotaInputPort getAdvancedQuotaInputPort;
    private final AdvancedHttpMapper mapper;

    @Inject
    public AdvancedResource(CreateAdvancedConversionInputPort createAdvancedConversionInputPort, GetAdvancedQuotaInputPort getAdvancedQuotaInputPort, AdvancedHttpMapper mapper) {
        this.createAdvancedConversionInputPort = createAdvancedConversionInputPort;
        this.getAdvancedQuotaInputPort = getAdvancedQuotaInputPort;
        this.mapper = mapper;
    }

    @POST
    @Path("/conversions")
    public Response create(@HeaderParam("Authorization") String authorizationHeader, @Valid CreateConversionHttpRequest request) {
        final AdvancedCreateConversionResponseModel response = createAdvancedConversionInputPort.execute(authorizationHeader, mapper.toApplication(request));
        return Response.status(Response.Status.ACCEPTED).entity(mapper.fromApplication(response)).build();
    }

    @GET
    @Path("/quota")
    public AdvancedQuotaHttpResponse quota(@HeaderParam("Authorization") String authorizationHeader) {
        final AdvancedQuotaResponseModel response = getAdvancedQuotaInputPort.execute(authorizationHeader);
        return mapper.fromApplication(response);
    }
}
