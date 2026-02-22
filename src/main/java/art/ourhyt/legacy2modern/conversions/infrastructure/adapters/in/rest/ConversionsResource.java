package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionStatusInputPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/conversions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConversionsResource {
    private final CreateConversionJobInputPort createConversionJobInputPort;
    private final GetConversionStatusInputPort getConversionStatusInputPort;
    private final ConversionsHttpMapper mapper;

    @Inject
    public ConversionsResource(CreateConversionJobInputPort createConversionJobInputPort, GetConversionStatusInputPort getConversionStatusInputPort, ConversionsHttpMapper mapper) {
        this.createConversionJobInputPort = createConversionJobInputPort;
        this.getConversionStatusInputPort = getConversionStatusInputPort;
        this.mapper = mapper;
    }

    @POST
    public Response create(@Valid CreateConversionHttpRequest request) {
        final CreateConversionResponseModel response = createConversionJobInputPort.execute(mapper.toApplication(request));
        return Response.status(Response.Status.ACCEPTED).entity(mapper.fromApplication(response)).build();
    }

    @GET
    @Path("/{jobId}")
    public GetConversionStatusHttpResponse getStatus(@PathParam("jobId") String jobId) {
        final GetConversionStatusResponseModel response = getConversionStatusInputPort.execute(jobId);
        return mapper.fromApplication(response);
    }
}
