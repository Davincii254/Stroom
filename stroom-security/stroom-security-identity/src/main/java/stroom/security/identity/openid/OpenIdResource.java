package stroom.security.identity.openid;

import stroom.security.openid.api.OpenId;
import stroom.security.openid.api.TokenResponse;
import stroom.util.shared.RestResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

@Singleton
@Tag(name = OpenIdResource.AUTHENTICATION_TAG)
@Path("/oauth2/v1/noauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OpenIdResource extends RestResource {

    String API_KEYS_TAG = "API Keys";
    String AUTHENTICATION_TAG = "Authentication";

    @Operation(
            summary = "Submit an OpenId AuthenticationRequest.",
            operationId = "openIdAuth")
    @GET
    @Path("auth")
    void auth(
            @Context HttpServletRequest request,
            @QueryParam(OpenId.SCOPE) @NotNull String scope,
            @QueryParam(OpenId.RESPONSE_TYPE) @NotNull String responseType,
            @QueryParam(OpenId.CLIENT_ID) @NotNull String clientId,
            @QueryParam(OpenId.REDIRECT_URI) @NotNull String redirectUri,
            @QueryParam(OpenId.NONCE) @Nullable String nonce,
            @QueryParam(OpenId.STATE) @Nullable String state,
            @QueryParam(OpenId.PROMPT) @Nullable String prompt);

    @Operation(
            summary = "Get a token from an access code or refresh token",
            operationId = "openIdToken")
    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    TokenResponse token(MultivaluedMap<String, String> formParams);

    @Operation(
            summary = "Provides access to this service's current public key. " +
                    "A client may use these keys to verify JWTs issued by this service.",
            tags = API_KEYS_TAG,
            operationId = "openIdCerts")
    @GET
    @Path("certs")
    Map<String, List<Map<String, Object>>> certs(@Context @NotNull HttpServletRequest httpServletRequest);

    @Operation(
            summary = "Provides discovery for openid configuration",
            tags = API_KEYS_TAG,
            operationId = "openIdConfiguration")
    @GET
    @Path(".well-known/openid-configuration")
    String openIdConfiguration();
}
