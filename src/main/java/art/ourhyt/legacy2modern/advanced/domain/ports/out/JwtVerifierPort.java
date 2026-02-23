package art.ourhyt.legacy2modern.advanced.domain.ports.out;

import art.ourhyt.legacy2modern.advanced.domain.model.AuthenticatedUser;

public interface JwtVerifierPort {
    AuthenticatedUser verifyAuthorizationHeader(String authorizationHeader);
}
