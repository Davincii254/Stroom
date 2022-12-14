package stroom.security.identity.account;

import stroom.security.api.ProcessingUserIdentityProvider;
import stroom.security.api.UserIdentity;
import stroom.security.identity.token.TokenBuilder;
import stroom.security.identity.token.TokenBuilderFactory;
import stroom.security.openid.api.OpenIdClientFactory;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ProcessingUserIdentityProviderImpl implements ProcessingUserIdentityProvider {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ProcessingUserIdentityProvider.class);
    private static final long ONE_DAY = TimeUnit.DAYS.toMillis(1);

    private final TokenBuilderFactory tokenBuilderFactory;
    private final OpenIdClientFactory openIdClientDetailsFactory;

    private final AtomicLong lastFetchTime = new AtomicLong(0);
    private volatile UserIdentity userIdentity;

    @Inject
    ProcessingUserIdentityProviderImpl(final TokenBuilderFactory tokenBuilderFactory,
                                       final OpenIdClientFactory openIdClientDetailsFactory) {
        this.tokenBuilderFactory = tokenBuilderFactory;
        this.openIdClientDetailsFactory = openIdClientDetailsFactory;
    }

    @Override
    public UserIdentity get() {
        final long now = System.currentTimeMillis();
        // Don't cache the user identity for more than a day in case its token expires.
        if (userIdentity == null || lastFetchTime.get() < now - ONE_DAY) {
            synchronized (this) {
                if (userIdentity == null || lastFetchTime.get() < now - ONE_DAY) {
                    final String token = createToken();
                    userIdentity = new ProcessingUserIdentity(token);
                    lastFetchTime.set(now);
                }
            }
        }

        return userIdentity;
    }

    @Override
    public boolean isProcessingUser(final UserIdentity userIdentity) {
        // It is possible that the passed user identity has a different jws than our
        // instance one but this is ok as we are regularly refreshing tokens for the
        // proc user and it has been authenticated at this point.
        if (userIdentity != null) {
            return userIdentity.equals(get());
        } else {
            LOGGER.debug("Null userIdentity");
            return false;
        }
    }

    private String createToken() {
        final Instant timeToExpiryInSeconds = LocalDateTime.now().plusYears(1).toInstant(ZoneOffset.UTC);
        final TokenBuilder tokenBuilder = tokenBuilderFactory
                .builder()
                .expirationTime(timeToExpiryInSeconds)
                .clientId(openIdClientDetailsFactory.getClient().getClientId())
                .subject(ProcessingUserIdentity.INTERNAL_PROCESSING_USER);
        return tokenBuilder.build();
    }
}
