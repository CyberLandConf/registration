package de.jugda.registration.api.authenticated;

import de.jugda.registration.TenantContext;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcSession;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;

/**
 * Logs the user out at Keycloak and brings them back to the admin area of the JUG they were working on.
 * <p>
 * Quarkus' built-in logout ({@code quarkus.oidc.logout.path}) is not used here: its
 * {@code post-logout-path} is a single static path, while the landing page has to carry the tenant. So the
 * RP-initiated logout request is assembled here instead -- {@code id_token_hint} identifies the session to
 * end, {@code post_logout_redirect_uri} the way back. The local session cookie is dropped before the
 * redirect, otherwise the browser would come back holding a session whose Keycloak counterpart is gone.
 * <p>
 * The landing page requires authentication, so Keycloak asks for credentials again right away -- which is
 * the point on the shared laptops these pages are used from.
 */
@Path("admin/{tenant}/logout")
@Authenticated
public class AdminLogoutResource {

    @Inject
    OidcSession oidcSession;

    @Inject
    OidcConfigurationMetadata oidcMetadata;

    @Inject
    TenantContext tenantCtx;

    @Context
    UriInfo uriInfo;

    @GET
    public Response logout() {
        URI landingPage = uriInfo.getBaseUriBuilder().path("admin").path(tenantCtx.getTenantId()).build();
        URI endSession = UriBuilder.fromUri(oidcMetadata.getEndSessionUri())
            .queryParam("id_token_hint", oidcSession.getIdToken().getRawToken())
            .queryParam("post_logout_redirect_uri", landingPage.toString())
            .build();
        // RESTEasy Classic cannot return a Uni -- it would try to serialize it as the response body.
        // Blocking is fine, the endpoint runs on a worker thread.
        oidcSession.logout().await().indefinitely();
        return Response.seeOther(endSession).build();
    }
}
