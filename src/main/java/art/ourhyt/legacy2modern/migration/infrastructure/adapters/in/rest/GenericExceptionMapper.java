package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        final ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorBody("INTERNAL_ERROR", "Unexpected server error", List.of()));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
