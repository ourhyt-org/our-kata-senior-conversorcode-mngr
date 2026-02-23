package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.security;

import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import art.ourhyt.legacy2modern.advanced.domain.model.AuthenticatedUser;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupabaseJwtVerifierAdapterTest {
    @Test
    void validatesTokenAndExtractsClaims() throws Exception {
        final RSAKey key = new RSAKeyGeneratorSupport().generate("kid-1");
        final String jwks = new JWKSet(key.toPublicJWK()).toString();

        final JwksHttpClient client = new FixedJwksClient(List.of(jwks));
        final JwksCache cache = new JwksCache(client, fixedClock());
        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(new FixedConfig("my-aud"), cache, fixedClock());

        final String token = signedToken(key, "kid-1", "https://example.supabase.co/auth/v1", "user-1", "a@a.com", "my-aud", Instant.parse("2026-02-24T00:00:00Z"));
        final AuthenticatedUser user = verifier.verifyAuthorizationHeader("Bearer " + token);

        assertEquals("user-1", user.userId());
        assertEquals("a@a.com", user.email());
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        final RSAKey jwksKey = new RSAKeyGeneratorSupport().generate("kid-1");
        final RSAKey signKey = new RSAKeyGeneratorSupport().generate("kid-1");
        final String jwks = new JWKSet(jwksKey.toPublicJWK()).toString();

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("my-aud"),
            new JwksCache(new FixedJwksClient(List.of(jwks)), fixedClock()),
            fixedClock()
        );

        final String token = signedToken(signKey, "kid-1", "https://example.supabase.co/auth/v1", "user-1", "a@a.com", "my-aud", Instant.parse("2026-02-24T00:00:00Z"));
        assertThrows(AdvancedUnauthorizedException.class, () -> verifier.verifyAuthorizationHeader("Bearer " + token));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        final RSAKey key = new RSAKeyGeneratorSupport().generate("kid-1");
        final String jwks = new JWKSet(key.toPublicJWK()).toString();

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("my-aud"),
            new JwksCache(new FixedJwksClient(List.of(jwks)), fixedClock()),
            fixedClock()
        );

        final String token = signedToken(key, "kid-1", "https://example.supabase.co/auth/v1", "user-1", "a@a.com", "my-aud", Instant.parse("2026-02-22T00:00:00Z"));
        assertThrows(AdvancedUnauthorizedException.class, () -> verifier.verifyAuthorizationHeader("Bearer " + token));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        final RSAKey key = new RSAKeyGeneratorSupport().generate("kid-1");
        final String jwks = new JWKSet(key.toPublicJWK()).toString();

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("my-aud"),
            new JwksCache(new FixedJwksClient(List.of(jwks)), fixedClock()),
            fixedClock()
        );

        final String token = signedToken(key, "kid-1", "https://wrong.issuer", "user-1", "a@a.com", "my-aud", Instant.parse("2026-02-24T00:00:00Z"));
        assertThrows(AdvancedUnauthorizedException.class, () -> verifier.verifyAuthorizationHeader("Bearer " + token));
    }

    @Test
    void rejectsAudienceMismatchWhenConfigured() throws Exception {
        final RSAKey key = new RSAKeyGeneratorSupport().generate("kid-1");
        final String jwks = new JWKSet(key.toPublicJWK()).toString();

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("expected"),
            new JwksCache(new FixedJwksClient(List.of(jwks)), fixedClock()),
            fixedClock()
        );

        final String token = signedToken(key, "kid-1", "https://example.supabase.co/auth/v1", "user-1", "a@a.com", "other", Instant.parse("2026-02-24T00:00:00Z"));
        assertThrows(AdvancedUnauthorizedException.class, () -> verifier.verifyAuthorizationHeader("Bearer " + token));
    }

    @Test
    void refreshesCacheWhenKidIsMissing() throws Exception {
        final RSAKey key = new RSAKeyGeneratorSupport().generate("kid-2");
        final String first = new JWKSet().toString();
        final String second = new JWKSet(key.toPublicJWK()).toString();
        final FixedJwksClient client = new FixedJwksClient(List.of(first, second));

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("my-aud"),
            new JwksCache(client, fixedClock()),
            fixedClock()
        );

        final String token = signedToken(key, "kid-2", "https://example.supabase.co/auth/v1", "user-2", null, "my-aud", Instant.parse("2026-02-24T00:00:00Z"));
        final AuthenticatedUser user = verifier.verifyAuthorizationHeader("Bearer " + token);

        assertEquals("user-2", user.userId());
        assertEquals(2, client.calls);
    }

    @Test
    void validatesEs256Token() throws Exception {
        final ECKey key = new ECKeyGeneratorSupport().generate("kid-ec-1");
        final String jwks = new JWKSet(key.toPublicJWK()).toString();

        final SupabaseJwtVerifierAdapter verifier = new SupabaseJwtVerifierAdapter(
            new FixedConfig("my-aud"),
            new JwksCache(new FixedJwksClient(List.of(jwks)), fixedClock()),
            fixedClock()
        );

        final String token = signedEcToken(key, "kid-ec-1", "https://example.supabase.co/auth/v1", "user-ec-1", "ec@a.com", "my-aud", Instant.parse("2026-02-24T00:00:00Z"));
        final AuthenticatedUser user = verifier.verifyAuthorizationHeader("Bearer " + token);

        assertEquals("user-ec-1", user.userId());
    }

    private String signedToken(RSAKey key, String kid, String issuer, String subject, String email, String audience, Instant expiration) throws JOSEException {
        final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .expirationTime(java.util.Date.from(expiration))
            .issueTime(java.util.Date.from(Instant.parse("2026-02-22T00:00:00Z")))
            .audience(audience);
        if (email != null) {
            claims.claim("email", email);
        }

        final SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
            claims.build()
        );
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private String signedEcToken(ECKey key, String kid, String issuer, String subject, String email, String audience, Instant expiration) throws JOSEException {
        final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .expirationTime(java.util.Date.from(expiration))
            .issueTime(java.util.Date.from(Instant.parse("2026-02-22T00:00:00Z")))
            .audience(audience);
        if (email != null) {
            claims.claim("email", email);
        }

        final SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).type(JOSEObjectType.JWT).build(),
            claims.build()
        );
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-02-23T00:00:00Z"), ZoneOffset.UTC);
    }

    private record FixedConfig(String audience) implements AdvancedConfigPort {
        @Override
        public String usageTable() {
            return "kata-advanced-usage-qa";
        }

        @Override
        public int defaultDailyLimit() {
            return 10;
        }

        @Override
        public String supabaseJwksUrl() {
            return "https://example.supabase.co/auth/v1/.well-known/jwks.json";
        }

        @Override
        public String supabaseIssuer() {
            return "https://example.supabase.co/auth/v1";
        }

        @Override
        public String supabaseAudience() {
            return audience;
        }
    }

    private static final class FixedJwksClient extends JwksHttpClient {
        private final List<String> payloads;
        private int index;
        private int calls;

        private FixedJwksClient(List<String> payloads) {
            this.payloads = payloads;
        }

        @Override
        public String fetchJwks(String jwksUrl) {
            calls++;
            if (index >= payloads.size()) {
                return payloads.getLast();
            }
            return payloads.get(index++);
        }
    }

    private static final class RSAKeyGeneratorSupport {
        RSAKey generate(String kid) {
            try {
                return new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID(kid).generate();
            } catch (JOSEException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class ECKeyGeneratorSupport {
        ECKey generate(String kid) {
            try {
                return new com.nimbusds.jose.jwk.gen.ECKeyGenerator(Curve.P_256).keyID(kid).generate();
            } catch (JOSEException exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
