package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AdvancedUnauthorizedExceptionMapper implements ExceptionMapper<AdvancedUnauthorizedException> {
    @Override
    public Response toResponse(AdvancedUnauthorizedException exception) {
        final ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorBody(exception.code(), exception.getMessage(), exception.details()));
        return Response.status(Response.Status.UNAUTHORIZED).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
