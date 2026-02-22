package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionValidationException;
import art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConversionValidationExceptionMapper implements ExceptionMapper<ConversionValidationException> {
    @Override
    public Response toResponse(ConversionValidationException exception) {
        final ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorBody(exception.code(), exception.getMessage(), exception.details()));
        return Response.status(Response.Status.BAD_REQUEST).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
