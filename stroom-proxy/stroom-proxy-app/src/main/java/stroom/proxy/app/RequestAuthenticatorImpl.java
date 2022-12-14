package stroom.proxy.app;

import stroom.security.api.RequestAuthenticator;
import stroom.security.api.UserIdentity;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;

import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class RequestAuthenticatorImpl implements RequestAuthenticator {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(RequestAuthenticatorImpl.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final ReceiveDataConfig receiveDataConfig;

    private final JwtConsumer jwtConsumer;

    @Inject
    public RequestAuthenticatorImpl(final ReceiveDataConfig receiveDataConfig) {
        this.receiveDataConfig = receiveDataConfig;
        jwtConsumer = newJwtConsumer();
    }

    @Override
    public Optional<UserIdentity> authenticate(final HttpServletRequest request) {
        Optional<UserIdentity> userIdentity = Optional.empty();

        if (receiveDataConfig.getPublicKey() != null && !receiveDataConfig.getPublicKey().isEmpty()) {
            // See if we can login with a token if one is supplied.
            try {
                final Optional<JwtContext> optionalJwtContext = getJwtContext(request);
                if (optionalJwtContext.isPresent()) {
                    userIdentity = optionalJwtContext.flatMap(jwtContext -> getUserIdentity(jwtContext));
                } else {
                    LOGGER.debug(() -> "No JWS found in headers in request to " + request.getRequestURI());
                }
            } catch (final RuntimeException e) {
                LOGGER.debug(e::getMessage, e);
            }
        }

        return userIdentity;
    }

    private Optional<JwtContext> getJwtContext(final HttpServletRequest request) {
        LOGGER.debug(() -> AUTHORIZATION_HEADER + "=" + request.getHeader(AUTHORIZATION_HEADER));

        final Optional<String> optionalJws = JwtUtil.getJwsFromHeader(request, AUTHORIZATION_HEADER);
        return optionalJws
                .flatMap(this::getJwtContext)
                .or(() -> {
                    LOGGER.debug(() -> "No JWS found in headers in request to " + request.getRequestURI());
                    return Optional.empty();
                });
    }

    /**
     * Verify the JSON Web Signature and then extract the user identity from it
     */
    private Optional<JwtContext> getJwtContext(final String jws) {
        if (jwtConsumer == null) {
            return Optional.empty();
        }

        Objects.requireNonNull(jws, "Null JWS");
        LOGGER.debug(() -> "Found auth header in request. It looks like this: " + jws);

        try {
            LOGGER.debug(() -> "Verifying token...");
            final JwtContext jwtContext = jwtConsumer.process(jws);

            // TODO : @66 Check against blacklist to see if token has been revoked. Blacklist
            //  is a list of JWI (JWT IDs) on auth service. Only tokens with `jwi` claims are API
            //  keys so only those tokens need checking against the blacklist cache.

//            if (checkTokenRevocation) {
//                LOGGER.debug(() -> "Checking token revocation status in remote auth service...");
//                final String userId = getUserIdFromToken(jws);
//                isRevoked = userId == null;
//            }

            return Optional.ofNullable(jwtContext);

        } catch (final RuntimeException | InvalidJwtException e) {
            LOGGER.debug(() -> "Unable to verify token: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private JwtConsumer newJwtConsumer() {
        JwtConsumer jwtConsumer = null;

        try {
            if (receiveDataConfig.getPublicKey() != null && !receiveDataConfig.getPublicKey().isBlank()) {
                // If we don't have a JWK we can't create a consumer to verify anything.
                // Why might we not have one? If the remote authentication service was down when Stroom started
                // then we wouldn't. It might not be up now but we're going to try and fetch it.
                final PublicJsonWebKey publicJsonWebKey =
                        RsaJsonWebKey.Factory.newPublicJwk(receiveDataConfig.getPublicKey());
                final JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(publicJsonWebKey);

                final VerificationKeyResolver verificationKeyResolver = new JwksVerificationKeyResolver(
                        jsonWebKeySet.getJsonWebKeys());

                final JwtConsumerBuilder builder = new JwtConsumerBuilder()
                        .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims
                        // to account for clock skew
                        .setRequireSubject() // the JWT must have a subject claim
                        .setVerificationKeyResolver(verificationKeyResolver)
                        .setExpectedAudience(receiveDataConfig.getClientId())
                        .setRelaxVerificationKeyValidation() // relaxes key length requirement
                        .setJwsAlgorithmConstraints(
// only allow the expected signature algorithm(s) in the given context
                                new AlgorithmConstraints(
                                        AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
                                        AlgorithmIdentifiers.RSA_USING_SHA256));
//                        .setExpectedIssuer(ResolvedOpenIdConfig.INTERNAL_ISSUER);
                jwtConsumer = builder.build();
            }
        } catch (final JoseException e) {
            LOGGER.error("Unable to parse public key from config: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        return jwtConsumer;
    }

    private Optional<UserIdentityImpl> getUserIdentity(final JwtContext jwtContext) {
        LOGGER.debug(() -> "Getting user identity from jwtContext=" + jwtContext);
        final String userId = getUserId(jwtContext.getJwtClaims());
        if (userId != null) {
            return Optional.of(new UserIdentityImpl(userId));
        }
        return Optional.empty();
    }

    private String getUserId(final JwtClaims jwtClaims) {
        LOGGER.trace("getUserId");
        String userId = JwtUtil.getEmail(jwtClaims);
        if (userId == null) {
            userId = JwtUtil.getUserIdFromIdentities(jwtClaims);
        }
        if (userId == null) {
            userId = JwtUtil.getUserName(jwtClaims);
        }
        if (userId == null) {
            userId = JwtUtil.getSubject(jwtClaims);
        }

        return userId;
    }
}
