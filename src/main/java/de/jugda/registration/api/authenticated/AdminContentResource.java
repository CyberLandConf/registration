package de.jugda.registration.api.authenticated;

import de.jugda.registration.TenantContext;
import de.jugda.registration.service.ContentService;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("admin/{tenant}/content")
@Produces(MediaType.TEXT_HTML)
@Authenticated
public class AdminContentResource {

    @Location("admin/content")
    Template content;

    @Inject
    ContentService contentService;

    @Inject
    TenantContext tenantCtx;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Context
    UriInfo uriInfo;

    @GET
    public TemplateInstance get() {
        return content
            .data("tenant", tenantCtx.getTenant())
            .data("texts", contentService.allTexts())
            .data("id", idToken)
            .data("activeNav", "content");
    }

    /**
     * The form field names are the content keys themselves ({@code registration.name} etc.), which is why the
     * form is taken as a map rather than as a BeanParam with hard-wired fields.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(MultivaluedMap<String, String> form) {
        contentService.save(form);
        return Response.status(Response.Status.FOUND)
            .location(uriInfo.getRequestUri())
            .build();
    }
}
