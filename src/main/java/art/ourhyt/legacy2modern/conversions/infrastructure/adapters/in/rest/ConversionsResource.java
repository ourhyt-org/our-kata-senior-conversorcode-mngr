package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesRequestModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionFilesInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionStatusInputPort;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/conversions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConversionsResource {
    private final CreateConversionJobInputPort createConversionJobInputPort;
    private final GetConversionStatusInputPort getConversionStatusInputPort;
    private final GetConversionFilesInputPort getConversionFilesInputPort;
    private final ConversionsHttpMapper mapper;

    @Inject
    public ConversionsResource(CreateConversionJobInputPort createConversionJobInputPort, GetConversionStatusInputPort getConversionStatusInputPort, GetConversionFilesInputPort getConversionFilesInputPort, ConversionsHttpMapper mapper) {
        this.createConversionJobInputPort = createConversionJobInputPort;
        this.getConversionStatusInputPort = getConversionStatusInputPort;
        this.getConversionFilesInputPort = getConversionFilesInputPort;
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

    @GET
    @Path("/{jobId}/files")
    public GetConversionFilesHttpResponse getFiles(
        @PathParam("jobId") String jobId,
        @QueryParam("includeContent") @DefaultValue("false") boolean includeContent,
        @QueryParam("paths") String paths,
        @QueryParam("maxFiles") @DefaultValue("30") int maxFiles,
        @QueryParam("maxTotalBytes") @DefaultValue("300000") int maxTotalBytes,
        @QueryParam("maxFileBytes") @DefaultValue("200000") int maxFileBytes
    ) {
        final List<String> parsedPaths = parsePaths(paths);
        final GetConversionFilesRequestModel request = new GetConversionFilesRequestModel(
            jobId,
            includeContent,
            parsedPaths,
            maxFiles,
            maxTotalBytes,
            maxFileBytes
        );
        final GetConversionFilesResponseModel response = getConversionFilesInputPort.execute(request);
        return mapper.fromApplication(response);
    }

    private List<String> parsePaths(String paths) {
        if (paths == null || paths.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(paths.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();
    }
}
