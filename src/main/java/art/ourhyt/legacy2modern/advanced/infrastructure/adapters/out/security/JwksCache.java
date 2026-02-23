package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class JwksCache {
    private final JwksHttpClient jwksHttpClient;
    private final Clock clock;

    private volatile Map<String, JWK> keysByKid = Map.of();
    private volatile Instant expiresAt = Instant.EPOCH;

    @Inject
    public JwksCache(JwksHttpClient jwksHttpClient) {
        this(jwksHttpClient, Clock.systemUTC());
    }

    JwksCache(JwksHttpClient jwksHttpClient, Clock clock) {
        this.jwksHttpClient = jwksHttpClient;
        this.clock = clock;
    }

    public JWK getKey(String jwksUrl, String kid) {
        final Map<String, JWK> current = loadIfExpired(jwksUrl);
        final JWK existing = current.get(kid);
        if (existing != null) {
            return existing;
        }
        return forceRefresh(jwksUrl).get(kid);
    }

    private Map<String, JWK> loadIfExpired(String jwksUrl) {
        if (Instant.now(clock).isBefore(expiresAt)) {
            return keysByKid;
        }
        synchronized (this) {
            if (Instant.now(clock).isBefore(expiresAt)) {
                return keysByKid;
            }
            keysByKid = loadJwks(jwksUrl);
            expiresAt = Instant.now(clock).plusSeconds(600);
            return keysByKid;
        }
    }

    private Map<String, JWK> forceRefresh(String jwksUrl) {
        synchronized (this) {
            keysByKid = loadJwks(jwksUrl);
            expiresAt = Instant.now(clock).plusSeconds(600);
            return keysByKid;
        }
    }

    private Map<String, JWK> loadJwks(String jwksUrl) {
        try {
            final JWKSet jwkSet = JWKSet.parse(jwksHttpClient.fetchJwks(jwksUrl));
            final Map<String, JWK> map = new HashMap<>();
            for (JWK jwk : jwkSet.getKeys()) {
                if (jwk.getKeyID() != null) {
                    map.put(jwk.getKeyID(), jwk);
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (ParseException exception) {
            return Map.of();
        }
    }
}
