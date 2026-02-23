package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.security;

import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import art.ourhyt.legacy2modern.advanced.domain.model.AuthenticatedUser;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.JwtVerifierPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class SupabaseJwtVerifierAdapter implements JwtVerifierPort {
    private final AdvancedConfigPort config;
    private final JwksCache jwksCache;
    private final Clock clock;

    @Inject
    public SupabaseJwtVerifierAdapter(AdvancedConfigPort config, JwksCache jwksCache) {
        this(config, jwksCache, Clock.systemUTC());
    }

    SupabaseJwtVerifierAdapter(AdvancedConfigPort config, JwksCache jwksCache, Clock clock) {
        this.config = config;
        this.jwksCache = jwksCache;
        this.clock = clock;
    }

    @Override
    public AuthenticatedUser verifyAuthorizationHeader(String authorizationHeader) {
        final String token = extractBearerToken(authorizationHeader);
        final SignedJWT jwt = parseJwt(token);
        verifySignature(jwt);
        final JWTClaimsSet claims = claims(jwt);
        verifyClaims(claims);

        final String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            throw unauthorized("Missing sub claim");
        }

        final Object emailClaim = claims.getClaim("email");
        final String email = emailClaim instanceof String emailValue && !emailValue.isBlank() ? emailValue : null;
        return new AuthenticatedUser(userId, email);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw unauthorized("Authorization header is required");
        }
        final String[] parts = authorizationHeader.trim().split("\\s+", 2);
        if (parts.length != 2 || !"bearer".equals(parts[0].toLowerCase(Locale.ROOT)) || parts[1].isBlank()) {
            throw unauthorized("Authorization header must be Bearer token");
        }
        return parts[1].trim();
    }

    private SignedJWT parseJwt(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException exception) {
            throw unauthorized("Malformed JWT");
        }
    }

    private void verifySignature(SignedJWT jwt) {
        final String kid = jwt.getHeader().getKeyID();
        if (kid == null || kid.isBlank()) {
            throw unauthorized("Missing kid header");
        }
        final JWK jwk = jwksCache.getKey(config.supabaseJwksUrl(), kid);
        final JWSVerifier verifier;
        if (jwk instanceof RSAKey rsaKey) {
            try {
                verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            } catch (JOSEException exception) {
                throw unauthorized("Unable to resolve signing key");
            }
        } else if (jwk instanceof ECKey ecKey) {
            try {
                verifier = new ECDSAVerifier(ecKey);
            } catch (JOSEException exception) {
                throw unauthorized("Unable to resolve signing key");
            }
        } else {
            throw unauthorized("Unable to resolve signing key");
        }
        try {
            if (!jwt.verify(verifier)) {
                throw unauthorized("Invalid JWT signature");
            }
        } catch (JOSEException exception) {
            throw unauthorized("Invalid JWT signature");
        }
    }

    private JWTClaimsSet claims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException exception) {
            throw unauthorized("Malformed JWT claims");
        }
    }

    private void verifyClaims(JWTClaimsSet claims) {
        final String expectedIssuer = trim(config.supabaseIssuer());
        final String jwksUrl = trim(config.supabaseJwksUrl());
        if (expectedIssuer == null || jwksUrl == null) {
            throw unauthorized("Supabase JWT config is missing");
        }

        final String issuer = claims.getIssuer();
        if (!expectedIssuer.equals(issuer)) {
            throw unauthorized("Issuer mismatch");
        }

        final Instant now = Instant.now(clock);
        if (claims.getExpirationTime() == null || now.isAfter(claims.getExpirationTime().toInstant())) {
            throw unauthorized("Token is expired");
        }

        final String expectedAudience = trim(config.supabaseAudience());
        if (expectedAudience != null) {
            final List<String> audience = claims.getAudience();
            if (audience == null || audience.stream().noneMatch(expectedAudience::equals)) {
                throw unauthorized("Audience mismatch");
            }
        }
    }

    private String trim(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private AdvancedUnauthorizedException unauthorized(String detail) {
        return new AdvancedUnauthorizedException("UNAUTHORIZED", "Invalid or missing bearer token", List.of(detail));
    }
}
