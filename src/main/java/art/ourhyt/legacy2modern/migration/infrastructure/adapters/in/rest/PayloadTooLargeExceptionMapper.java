package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.migration.application.exceptions.PayloadTooLargeException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PayloadTooLargeExceptionMapper implements ExceptionMapper<PayloadTooLargeException> {
    @Override
    public Response toResponse(PayloadTooLargeException exception) {
        final ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorBody(exception.code(), exception.getMessage(), exception.details()));
        return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
