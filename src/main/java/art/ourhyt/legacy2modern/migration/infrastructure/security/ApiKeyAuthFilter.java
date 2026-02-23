package art.ourhyt.legacy2modern.migration.infrastructure.security;

import art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuthFilter implements ContainerRequestFilter {
    @ConfigProperty(name = "migration.api-key")
    String configuredApiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String requestPath = requestContext.getUriInfo().getRequestUri().getPath();
        if (requestPath != null && requestPath.contains("/advanced/")) {
            return;
        }
        final String providedApiKey = requestContext.getHeaderString("X-API-KEY");
        if (providedApiKey == null || !providedApiKey.equals(configuredApiKey)) {
            final ErrorResponse body = new ErrorResponse(new ErrorResponse.ErrorBody("UNAUTHORIZED", "Invalid or missing API key", List.of("X-API-KEY header is required")));
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(body).type(MediaType.APPLICATION_JSON).build());
        }
    }
}
