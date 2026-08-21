package de.jugda.registration.api.authenticated;

import de.jugda.registration.TenantContext;
import de.jugda.registration.service.TenantProvisioningException;
import de.jugda.registration.service.TenantProvisioningService;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Creating a JUG affects every other JUG's neighbourhood in this deployment, so it stays with the
 * operators: {@code admin} on top of the tenant role the {@link de.jugda.registration.TenantAccessFilter}
 * already demands.
 */
@Path("admin/{tenant}/tenants")
@Produces(MediaType.TEXT_HTML)
@RolesAllowed("admin")
public class AdminTenantProvisioningResource {

    @Location("admin/tenants")
    Template tenants;

    @Inject
    TenantProvisioningService provisioningService;

    @Inject
    TenantContext tenantCtx;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Context
    UriInfo uriInfo;

    @GET
    public TemplateInstance get(@QueryParam("created") String created) {
        return page(null, null, created, null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(@FormParam("id") String id, @FormParam("name") String name) {
        String created;
        try {
            created = provisioningService.create(id, name);
        } catch (TenantProvisioningException e) {
            // Hand the entered values back, so a typo in the id does not cost the whole form
            return Response.ok(page(id, name, null, e.getMessage())).build();
        }
        // Redirect after post, otherwise a reload tries to create the same JUG again
        return Response.status(Response.Status.FOUND)
            .location(uriInfo.getRequestUriBuilder().queryParam("created", created).build())
            .build();
    }

    private TemplateInstance page(String id, String name, String created, String error) {
        return tenants
            .data("tenant", tenantCtx.getTenant())
            .data("id", idToken)
            .data("templateTenant", TenantProvisioningService.TEMPLATE_TENANT)
            .data("newTenantId", id)
            .data("newTenantName", name)
            .data("created", created)
            .data("error", error);
    }
}
