package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.security;

import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class JwksHttpClient {
    private final HttpClient httpClient;

    public JwksHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    JwksHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String fetchJwks(String jwksUrl) {
        try {
            final HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUrl)).GET().timeout(Duration.ofSeconds(5)).build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unauthorized("Unable to fetch JWKS");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unauthorized("Unable to fetch JWKS");
        } catch (IOException exception) {
            throw unauthorized("Unable to fetch JWKS");
        } catch (IllegalArgumentException exception) {
            throw unauthorized("Invalid JWKS URL");
        }
    }

    private AdvancedUnauthorizedException unauthorized(String detail) {
        return new AdvancedUnauthorizedException("UNAUTHORIZED", "Invalid or missing bearer token", List.of(detail));
    }
}
